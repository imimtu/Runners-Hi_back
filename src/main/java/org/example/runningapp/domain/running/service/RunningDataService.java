package org.example.runningapp.domain.running.service;

import org.example.runningapp.domain.running.dto.RunningFeature;
import org.example.runningapp.domain.running.dto.RunningProperties;
import org.example.runningapp.domain.running.dto.RunningDataRequest;
import org.example.runningapp.domain.running.dto.RunningDataResponse;
import org.example.runningapp.domain.running.dto.RunningSessionSummary;
import org.example.runningapp.domain.running.entity.RunningSession;
import org.example.runningapp.domain.running.repository.RunningSessionRepository;
import org.example.runningapp.common.exception.InvalidRunningDataException;
import org.example.runningapp.common.exception.RunningSessionNotFoundException;
import org.example.runningapp.common.exception.ExternalServiceException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunningDataService {

	private final RunningSessionRepository repository;
	private final MongoTemplate mongoTemplate;

	/**
	 * 다음 세션 번호 조회 - 성능 최적화된 필드 선택
	 */
	public Integer getNextSessionNumber(Long userId) {
		if (userId == null || userId <= 0) {
			throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다");
		}

		try {
			return repository.findSessionNumOnlyByUserId(userId)
				.map(session -> session.getSessionNum() + 1)
				.orElse(1);
		} catch (Exception e) {
			log.error("세션 번호 조회 실패 - 사용자: {}, 오류: {}", userId, e.getMessage());
			throw new ExternalServiceException("세션 번호를 가져오는 중 오류가 발생했습니다", e);
		}
	}

	/**
	 * 러닝 데이터 저장 - 하이브리드 방식 (JPA + MongoTemplate)
	 */
	public RunningDataResponse saveRunningData(RunningDataRequest request, Long userId) {
		// 1. 입력 데이터 검증
		validateRunningDataRequest(request, userId);

		try {
			String sessionKey = generateSessionKey(userId, request.sessionNum());

			log.info("러닝 데이터 저장 시작 - 사용자: {}, sessionKey: {}, Feature 수: {}",
				userId, sessionKey, request.getFeatureCount());

			// JPA: 간단한 존재 여부 확인
			Optional<RunningSession> existingSession = repository.findByUserIdAndSessionKey(userId, sessionKey);

			if (existingSession.isEmpty()) {
				return createNewSession(userId, sessionKey, request);
			} else {
				return appendToExistingSessionOptimized(existingSession.get(), request);
			}

		} catch (InvalidRunningDataException e) {
			throw e; // 비즈니스 예외는 그대로 전파
		} catch (Exception e) {
			log.error("러닝 데이터 저장 실패 - 사용자: {}, 세션번호: {}, 오류: {}",
				userId, request.sessionNum(), e.getMessage(), e);
			throw new ExternalServiceException("러닝 데이터 저장 중 오류가 발생했습니다", e);
		}
	}

	/**
	 * JPA: 새 세션 생성 (단순 저장)
	 */
	private RunningDataResponse createNewSession(Long userId, String sessionKey, RunningDataRequest request) {
		try {
			log.info("새 세션 생성 - sessionKey: {}", sessionKey);

			List<Map<String, Object>> features = convertFeaturesToMapList(request.geoData().features());

			RunningSession newSession = RunningSession.builder()
				.userId(userId)
				.sessionKey(sessionKey)
				.sessionNum(request.sessionNum())
				.createdAt(LocalDateTime.now())
				.geoDataFeatures(features)
				.build();

			repository.save(newSession);

			return RunningDataResponse.success(
				request.getFeatureCount(),
				request.getTotalCoordinateCount(),
				RunningSessionSummary.fromFeatures(request.geoData().features())
			);

		} catch (Exception e) {
			log.error("새 세션 생성 실패 - sessionKey: {}, 오류: {}", sessionKey, e.getMessage());
			throw new ExternalServiceException("새 러닝 세션 생성 중 오류가 발생했습니다", e);
		}
	}

	/**
	 * ⚡ MongoTemplate: 성능 최적화 - 부분 업데이트만
	 */
	private RunningDataResponse appendToExistingSessionOptimized(RunningSession existingSession, RunningDataRequest request) {
		String sessionKey = existingSession.getSessionKey();
		log.info("기존 세션에 추가 (최적화) - sessionKey: {}, 기존: {}, 추가: {}",
			sessionKey, existingSession.getCurrentFeatureCount(), request.getFeatureCount());

		try {
			List<Map<String, Object>> newFeatures = convertFeaturesToMapList(request.geoData().features());

			// MongoTemplate: MongoDB $push 연산으로 부분 업데이트만 실행
			Query query = Query.query(Criteria.where("sessionKey").is(sessionKey));
			Update update = new Update().push("geoDataFeatures").each(newFeatures.toArray());

			mongoTemplate.updateFirst(query, update, RunningSession.class);

			return RunningDataResponse.success(
				request.getFeatureCount(),
				request.getTotalCoordinateCount(),
				RunningSessionSummary.fromFeatures(request.geoData().features())
			);

		} catch (Exception e) {
			log.error("세션 데이터 추가 실패 - sessionKey: {}, 오류: {}", sessionKey, e.getMessage());
			throw new ExternalServiceException("기존 러닝 세션에 데이터 추가 중 오류가 발생했습니다", e);
		}
	}

	/**
	 * JPA: 사용자 세션 목록 조회
	 */
	public List<RunningSession> getUserSessions(Long userId, int limit) {
		if (userId == null || userId <= 0) {
			throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다");
		}

		// 이후 추가 가능
		if (limit <= 0 || limit > 100) {
			throw new IllegalArgumentException("조회 개수는 1-100 사이여야 합니다");
		}

		try {
			return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit));
		} catch (Exception e) {
			log.error("사용자 세션 목록 조회 실패 - 사용자: {}, 제한: {}, 오류: {}", userId, limit, e.getMessage());
			throw new ExternalServiceException("러닝 세션 목록을 가져오는 중 오류가 발생했습니다", e);
		}
	}

	/**
	 * JPA: 특정 세션 조회
	 */
	public RunningSession getSessionByKey(Long userId, String sessionKey) {
		if (userId == null || userId <= 0) {
			throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다");
		}

		if (sessionKey == null || sessionKey.trim().isEmpty()) {
			throw new IllegalArgumentException("세션 키는 필수입니다");
		}

		try {
			return repository.findByUserIdAndSessionKey(userId, sessionKey)
				.orElseThrow(() -> new RunningSessionNotFoundException(
					String.format("세션을 찾을 수 없습니다. sessionKey: %s", sessionKey)
				));
		} catch (RunningSessionNotFoundException e) {
			throw e; // 비즈니스 예외는 그대로 전파
		} catch (Exception e) {
			log.error("세션 조회 실패 - 사용자: {}, sessionKey: {}, 오류: {}", userId, sessionKey, e.getMessage());
			throw new ExternalServiceException("러닝 세션을 가져오는 중 오류가 발생했습니다", e);
		}
	}

	/**
	 * 러닝 데이터 요청 유효성 검증
	 */
	private void validateRunningDataRequest(RunningDataRequest request, Long userId) {
		if (userId == null || userId <= 0) {
			throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다");
		}

		if (request.sessionNum() == null || request.sessionNum() < 1) {
			throw new InvalidRunningDataException("세션 번호는 1 이상이어야 합니다");
		}

		if (request.sessionNum() > 999999) {
			throw new InvalidRunningDataException("세션 번호가 너무 큽니다 (최대: 999999)");
		}

		if (request.geoData() == null) {
			throw new InvalidRunningDataException("GPS 데이터가 없습니다");
		}

		if (request.geoData().features() == null || request.geoData().features().isEmpty()) {
			throw new InvalidRunningDataException("GPS 좌표 정보가 없습니다");
		}

		// 데이터 크기 제한 체크(현재 1000개)
		if (request.getFeatureCount() > 1000) {
			throw new InvalidRunningDataException("한 번에 처리할 수 있는 GPS 포인트는 최대 1000개입니다");
		}

		// GPS 데이터 유효성 검증
		validateGpsCoordinates(request);

		// 시간 데이터 검증
		validateTimestamps(request);
	}

	/**
	 * GPS 좌표 유효성 검증
	 */
	private void validateGpsCoordinates(RunningDataRequest request) {
		try {
			request.geoData().features().forEach(feature -> {
				if (feature.geometry() == null) {
					throw new InvalidRunningDataException("GPS geometry 정보가 없습니다");
				}

				var coordinates = feature.geometry().coordinates();

				if (coordinates == null || coordinates.isEmpty()) {
					throw new InvalidRunningDataException("GPS 좌표가 비어있습니다");
				}

				coordinates.forEach(coord -> {
					if (coord == null || coord.size() < 2) {
						throw new InvalidRunningDataException("GPS 좌표 형식이 올바르지 않습니다 (경도, 위도 필요)");
					}

					try {
						double longitude = coord.get(0);
						double latitude = coord.get(1);

						// 전 세계 좌표 범위 체크
						if (longitude < -180.0 || longitude > 180.0) {
							throw new InvalidRunningDataException(
								String.format("경도 값이 유효하지 않습니다: %.6f (범위: -180 ~ 180)", longitude)
							);
						}

						if (latitude < -90.0 || latitude > 90.0) {
							throw new InvalidRunningDataException(
								String.format("위도 값이 유효하지 않습니다: %.6f (범위: -90 ~ 90)", latitude)
							);
						}

						// 대한민국 근처 좌표 범위 체크 (경고만)
						if (longitude < 124.0 || longitude > 132.0 ||
							latitude < 33.0 || latitude > 39.0) {
							log.warn("한국 외 지역 GPS 좌표 감지 - 사용자: {}, 경도: {}, 위도: {}",
								request.sessionNum(), longitude, latitude);
						}

					} catch (NumberFormatException | IndexOutOfBoundsException e) {
						throw new InvalidRunningDataException("GPS 좌표 값을 숫자로 변환할 수 없습니다");
					}
				});
			});
		} catch (InvalidRunningDataException e) {
			throw e;
		} catch (Exception e) {
			log.error("GPS 좌표 검증 중 예상치 못한 오류: {}", e.getMessage());
			RuntimeException validationException = new RuntimeException("GPS 좌표 검증 중 내부 오류", e);
			throw new ExternalServiceException("GPS 데이터 검증 중 오류가 발생했습니다", validationException);
		}
	}

	/**
	 * 타임스탬프 유효성 검증
	 */
	private void validateTimestamps(RunningDataRequest request) {
		try {
			long currentTime = System.currentTimeMillis();
			long oneHourAgo = currentTime - (60 * 60 * 1000); // 1시간 전
			long oneHourLater = currentTime + (60 * 60 * 1000); // 1시간 후

			for (RunningFeature feature : request.geoData().features()) {
				if (feature.properties() == null) {
					throw new InvalidRunningDataException("러닝 속성 정보가 없습니다");
				}

				RunningProperties props = feature.properties();

				if (props.timestampStart() == null || props.timestampEnd() == null) {
					throw new InvalidRunningDataException("시작 시간과 종료 시간은 필수입니다");
				}

				if (props.timestampStart() >= props.timestampEnd()) {
					throw new InvalidRunningDataException("시작 시간이 종료 시간보다 늦을 수 없습니다");
				}

				// 현실적인 시간 범위 체크 (1시간 전후)
				if (props.timestampStart() < oneHourAgo || props.timestampStart() > oneHourLater) {
					log.warn("비정상적인 타임스탬프 감지 - 세션: {}, 시작시간: {}",
						request.sessionNum(), props.timestampStart());
				}

				// 구간 시간이 너무 긴 경우 체크
				long duration = props.timestampEnd() - props.timestampStart();
				if (duration > 300000) { // 5분 = 300,000ms
					throw new InvalidRunningDataException("GPS 포인트 간 시간 간격이 너무 큽니다 (최대: 5분)");
				}
			}
		} catch (InvalidRunningDataException e) {
			throw e;
		} catch (Exception e) {
			log.error("타임스탬프 검증 중 예상치 못한 오류: {}", e.getMessage());
			RuntimeException validationException = new RuntimeException("타임스탬프 검증 중 내부 오류", e);
			throw new ExternalServiceException("시간 데이터 검증 중 오류가 발생했습니다", validationException);
		}
	}

	/**
	 * sessionKey 생성
	 */
	private String generateSessionKey(Long userId, Integer sessionNum) {
		return userId + "-" + sessionNum;
	}

	/**
	 * RunningFeature 리스트를 Map 리스트로 변환 (MongoDB 저장용)
	 */
	private List<Map<String, Object>> convertFeaturesToMapList(List<RunningFeature> features) {
		try {
			return features.stream()
				.map(this::convertFeatureToMap)
				.toList();
		} catch (Exception e) {
			log.error("Feature 데이터 변환 실패: {}", e.getMessage());
			throw new ExternalServiceException("러닝 데이터 형식 변환 중 오류가 발생했습니다", e);
		}
	}

	private Map<String, Object> convertFeatureToMap(RunningFeature feature) {
		try {
			return Map.of(
				"type", feature.type(),
				"properties", convertPropertiesToMap(feature.properties()),
				"geometry", Map.of(
					"type", feature.geometry().type(),
					"coordinates", feature.geometry().coordinates()
				)
			);
		} catch (Exception e) {
			log.error("단일 Feature 변환 실패: {}", e.getMessage());
			throw new RuntimeException("Feature 데이터 변환 실패", e);
		}
	}

	private Map<String, Object> convertPropertiesToMap(RunningProperties props) {
		try {
			Map<String, Object> map = new java.util.HashMap<>();
			map.put("timestampStart", props.timestampStart());
			map.put("timestampEnd", props.timestampEnd());

			// null이 아닌 값들만 추가
			if (props.height() != null) map.put("height", props.height());
			if (props.bpm() != null) map.put("bpm", props.bpm());
			if (props.pace() != null) map.put("pace", props.pace());
			if (props.power() != null) map.put("power", props.power());
			if (props.cadence() != null) map.put("cadence", props.cadence());
			if (props.minVerticalAmplitude() != null) map.put("minVerticalAmplitude", props.minVerticalAmplitude());
			if (props.maxVerticalAmplitude() != null) map.put("maxVerticalAmplitude", props.maxVerticalAmplitude());
			if (props.minGct() != null) map.put("minGct", props.minGct());
			if (props.maxGct() != null) map.put("maxGct", props.maxGct());
			if (props.stride() != null) map.put("stride", props.stride());

			return map;
		} catch (Exception e) {
			log.error("Properties 변환 실패: {}", e.getMessage());
			throw new RuntimeException("Properties 데이터 변환 실패", e);
		}
	}
}