package org.example.runningapp.domain.running.service;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.*;

import org.example.runningapp.domain.running.dto.RunningFeature;
import org.example.runningapp.domain.running.dto.RunningProperties;
import org.example.runningapp.domain.running.dto.RunningDataRequest;
import org.example.runningapp.domain.running.dto.RunningDataResponse;
import org.example.runningapp.domain.running.dto.RunningSessionSummary;
import org.example.runningapp.domain.running.entity.RunningSession;
import org.example.runningapp.domain.running.repository.RunningSessionRepository;
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

	private final RunningSessionRepository repository;      // 간단한 CRUD
	private final MongoTemplate mongoTemplate;              // 성능 중요한 부분

	/**
	 *  JPA: 간단한 조회 - 성능 최적화된 필드 선택
	 */
	public Integer getNextSessionNumber(Long userId) {
		return repository.findSessionNumOnlyByUserId(userId)
			.map(session -> session.getSessionNum() + 1)
			.orElse(1);
	}

	/**
	 * 러닝 데이터 저장 - 하이브리드 방식
	 */
	public RunningDataResponse saveRunningData(RunningDataRequest request, Long userId) {
		try {
			String sessionKey = generateSessionKey(userId, request.sessionNum());

			log.info("러닝 데이터 저장 - 사용자: {}, sessionKey: {}, Feature 수: {}",
				userId, sessionKey, request.getFeatureCount());

			// JPA: 간단한 존재 여부 확인
			Optional<RunningSession> existingSession = repository.findByUserIdAndSessionKey(userId, sessionKey);

			if (existingSession.isEmpty()) {
				return createNewSession(userId, sessionKey, request);
			} else {
				return appendToExistingSessionOptimized(existingSession.get(), request);
			}

		} catch (Exception e) {
			log.error("러닝 데이터 저장 실패 - 사용자: {}, 오류: {}", userId, e.getMessage(), e);
			return RunningDataResponse.error("저장 실패: " + e.getMessage());
		}
	}

	/**
	 * JPA: 새 세션 생성 (단순 저장)
	 */
	private RunningDataResponse createNewSession(Long userId, String sessionKey, RunningDataRequest request) {
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
	}

	/**
	 * ⚡ MongoTemplate: 성능 최적화 - 부분 업데이트만
	 */
	private RunningDataResponse appendToExistingSessionOptimized(RunningSession existingSession, RunningDataRequest request) {
		String sessionKey = existingSession.getSessionKey();
		Long userId = existingSession.getUserId();

		log.info("기존 세션에 추가 (최적화) - sessionKey: {}, 기존: {}, 추가: {}",
			sessionKey, existingSession.getCurrentFeatureCount(), request.getFeatureCount());

		List<Map<String, Object>> newFeatures = convertFeaturesToMapList(request.geoData().features());

		// userId + sessionKey 로 확정적인 타겟팅
		Query query = Query.query(
			Criteria.where("userId").is(userId)
				.and("sessionKey").is(sessionKey)
		);

		// 없으면 생성(setOnInsert), 있으면 push 로 원자적 갱신
		Update update = new Update()
			.setOnInsert("userId", userId)
			.setOnInsert("sessionKey", sessionKey)
			.setOnInsert("sessionNum", request.sessionNum())
			.setOnInsert("createdAt", LocalDateTime.now())
			.push("geoDataFeatures").each(newFeatures.toArray());

		var options =
			options()
				.upsert(true)
				.returnNew(true);

		RunningSession updated =
			mongoTemplate.findAndModify(query, update, options, RunningSession.class);

		if (updated == null) {
			log.error("세션 업데이트 실패(FindAndModify null) - userId: {}, sessionKey: {}", userId, sessionKey);
			return RunningDataResponse.error("세션 업데이트 실패");
		}

		return RunningDataResponse.success(
			request.getFeatureCount(),
			request.getTotalCoordinateCount(),
			RunningSessionSummary.fromFeatures(request.geoData().features())
		);
	}


	/**
	 *  JPA: 단순 조회들
	 */
	public List<RunningSession> getUserSessions(Long userId, int limit) {
		return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit));
	}

	/**
	 *  JPA: 단순 조회
	 */
	public RunningSession getSessionByKey(Long userId, String sessionKey) {
		return repository.findByUserIdAndSessionKey(userId, sessionKey)
			.orElse(null);
	}

	/**
	 * sessionKey 생성
	 */
	private String generateSessionKey(Long userId, Integer sessionNum) {
		return userId + "-" + sessionNum;
	}

	private List<Map<String, Object>> convertFeaturesToMapList(List<RunningFeature> features) {
		return features.stream()
			.map(this::convertFeatureToMap)
			.toList();
	}

	private Map<String, Object> convertFeatureToMap(RunningFeature feature) {
		return Map.of(
			"type", feature.type(),
			"properties", convertPropertiesToMap(feature.properties()),
			"geometry", Map.of(
				"type", feature.geometry().type(),
				"coordinates", feature.geometry().coordinates()
			)
		);
	}

	private Map<String, Object> convertPropertiesToMap(RunningProperties props) {
		Map<String, Object> map = new java.util.HashMap<>();
		map.put("timestampStart", props.timestampStart());
		map.put("timestampEnd", props.timestampEnd());

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
	}
}