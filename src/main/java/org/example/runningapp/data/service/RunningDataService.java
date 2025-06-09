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

@Slf4j
@Service
@RequiredArgsConstructor
public class RunningDataService {

	private final MongoTemplate mongoTemplate;

	public RunningDataResponse saveRunningData(RunningDataRequest request, Long userId, String fullSessionId) {
		try {
			// GeoJSON을 Map으로 변환 (MongoDB에 유연하게 저장)
			Map<String, Object> geoJsonMap = convertToMap(request.geoData());

			// 세션 요약 정보 생성
			RunningSessionSummary summaryDto = RunningSessionSummary.fromFeatures(
				request.geoData().features()
			);

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

			// MongoDB에 저장
			RunningSession session = RunningSession.builder()
				.userId(userId)  // Controller에서 전달받은 JWT의 userId
				.sessionId(fullSessionId)  // 완전한 sessionId (userId-sessionNum-timestamp)
				.geoJsonData(geoJsonMap)
				.summary(summary)
				.createdAt(LocalDateTime.now())
				.build();

			mongoTemplate.save(session);

			log.info("러닝 데이터 저장 완료 - 사용자: {}, 세션번호: {}, 타임스탬프: {}, 완전세션: {}, Feature 수: {}, 좌표 수: {}",
				userId, request.sessionNum(), request.startTimestamp(), fullSessionId,
				request.getFeatureCount(), request.getTotalCoordinateCount());

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