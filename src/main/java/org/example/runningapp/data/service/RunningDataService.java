package org.example.runningapp.data.service;

import org.example.runningapp.data.dto.GeoJsonFeatureCollection;
import org.example.runningapp.data.dto.RunningFeature;
import org.example.runningapp.data.dto.RunningProperties;
import org.example.runningapp.data.dto.reqres.RunningDataRequest;
import org.example.runningapp.data.dto.reqres.RunningDataResponse;
import org.example.runningapp.data.dto.reqres.RunningSessionSummary;
import org.example.runningapp.data.entity.RunningSession;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunningDataService {

	private final MongoTemplate mongoTemplate;

	public RunningDataResponse saveRunningData(RunningDataRequest request, Long userId) {
		try {
			// ✅ 1. userId + sessionNum으로 기존 세션 찾기
			RunningSession existingSession = findSessionByUserAndNum(userId, request.sessionNum());

			if (existingSession != null) {
				// ✅ 2-1. 기존 세션에 데이터 추가
				log.info("기존 세션에 데이터 추가 - 사용자: {}, 세션: {}", userId, existingSession.getSessionId());
				appendDataToExistingSession(existingSession, request);
			} else {
				// ✅ 2-2. 새 세션 생성
				String newSessionId = createSessionId(userId, request.sessionNum(), request);
				log.info("새 세션 생성 - 사용자: {}, 세션: {}", userId, newSessionId);
				existingSession = createNewSession(userId, newSessionId, request);
			}

			// ✅ 3. 응답 생성
			RunningSessionSummary summaryDto = RunningSessionSummary.fromFeatures(request.geoData().features());

			return RunningDataResponse.success(
				request.getFeatureCount(),
				request.getTotalCoordinateCount(),
				summaryDto
			);

		} catch (Exception e) {
			log.error("러닝 데이터 저장 실패: {}", e.getMessage(), e);
			return RunningDataResponse.error("저장 실패: " + e.getMessage());
		}
	}

	private RunningSession findSessionByUserAndNum(Long userId, Integer sessionNum) {
		String sessionPrefix = userId + "-" + sessionNum + "-";
		Query query = Query.query(
			Criteria.where("userId").is(userId)
				.and("sessionId").regex("^" + Pattern.quote(sessionPrefix))
		);
		return mongoTemplate.findOne(query, RunningSession.class);
	}

	private String createSessionId(Long userId, Integer sessionNum, RunningDataRequest request) {
		Long firstTimestamp = request.geoData().features().get(0).properties().timestampStart();
		return userId + "-" + sessionNum + "-" + firstTimestamp;
	}

	private RunningSession findActiveSession(Long userId, String potentialSessionId) {
		// 1. 정확한 sessionId로 먼저 찾기
		Query exactQuery = Query.query(
			Criteria.where("userId").is(userId)
				.and("sessionId").is(potentialSessionId)
		);

		RunningSession session = mongoTemplate.findOne(exactQuery, RunningSession.class);

		if (session != null) {
			return session;
		}

		// 2. 같은 sessionNum을 가진 세션이 있는지 확인 (오늘 날짜 기준)
		String sessionPrefix = userId + "-" + extractSessionNum(potentialSessionId) + "-";
		Query prefixQuery = Query.query(
			Criteria.where("userId").is(userId)
				.and("sessionId").regex("^" + Pattern.quote(sessionPrefix))
				.and("createdAt").gte(LocalDateTime.now().withHour(0).withMinute(0).withSecond(0))
		);

		return mongoTemplate.findOne(prefixQuery, RunningSession.class);
	}

	private Integer extractSessionNum(String sessionId) {
		String[] parts = sessionId.split("-");
		return Integer.parseInt(parts[1]);
	}

	private void appendDataToExistingSession(RunningSession session, RunningDataRequest request) {
		// 1. 새 Feature들을 Map으로 변환
		List<Map<String, Object>> newFeatures = convertFeaturesToMapList(request.geoData().features());

		// 2. 기존 세션에 데이터 추가
		session.appendGeoData(newFeatures);

		// 3. MongoDB에 저장 (UPDATE)
		mongoTemplate.save(session);
	}

	private RunningSession createNewSession(Long userId, String fullSessionId, RunningDataRequest request) {
		// GeoJSON 데이터 변환
		Map<String, Object> geoJsonMap = convertToMap(request.geoData());

		// 세션 요약 정보 생성
		RunningSessionSummary summaryDto = RunningSessionSummary.fromFeatures(request.geoData().features());

		RunningSession.SessionSummary summary = RunningSession.SessionSummary.builder()
			.sessionStartTime(summaryDto.sessionStartTime())
			.sessionEndTime(summaryDto.sessionEndTime())
			.durationSeconds(summaryDto.durationSeconds())
			.featureCount(request.getFeatureCount())
			.totalCoordinateCount(request.getTotalCoordinateCount())
			.avgPace(summaryDto.avgPaceKmh())
			.avgBpm(summaryDto.avgBpm())
			.maxHeight(summaryDto.maxHeight())
			.minHeight(summaryDto.minHeight())
			.build();

		// 새 세션 생성
		RunningSession session = RunningSession.builder()
			.userId(userId)
			.sessionId(fullSessionId)
			.geoJsonData(geoJsonMap)
			.summary(summary)
			.createdAt(LocalDateTime.now())
			.build();

		// MongoDB에 저장 (INSERT)
		mongoTemplate.save(session);

		return session;
	}

	private List<Map<String, Object>> convertFeaturesToMapList(List<RunningFeature> features) {
		return features.stream()
			.map(this::convertFeatureToMap)
			.toList();
	}

	public List<RunningSession> getUserSessions(Long userId, int limit) {
		Query query = Query.query(Criteria.where("userId").is(userId))
			.with(Sort.by(Sort.Direction.DESC, "createdAt"))
			.limit(limit);

		return mongoTemplate.find(query, RunningSession.class);
	}

	public RunningSession getSessionData(Long userId, String sessionId) {
		Query query = Query.query(
			Criteria.where("userId").is(userId)
				.and("sessionId").is(sessionId)
		);

		return mongoTemplate.findOne(query, RunningSession.class);
	}

	// GeoJSON DTO를 Map으로 변환 (MongoDB 저장용)
	private Map<String, Object> convertToMap(GeoJsonFeatureCollection geoData) {
		return Map.of(
			"type", geoData.type(),
			"features", geoData.features().stream()
				.map(this::convertFeatureToMap)
				.toList()
		);
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
	}
}