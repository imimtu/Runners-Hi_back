package org.example.runningapp.data.entity;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "running_sessions")
@CompoundIndex(def = "{'userId': 1, 'sessionId': 1}")
@CompoundIndex(def = "{'userId': 1, 'createdAt': -1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunningSession {

	@Id
	private String id;

	private Long userId;
	private String sessionId;
	private LocalDateTime createdAt;

	// GeoJSON 데이터를 Map으로 저장 (유연성 확보)
	private Map<String, Object> geoJsonData;

	// 세션 요약 정보
	private SessionSummary summary;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class SessionSummary {
		private Long sessionStartTime;      // 세션 시작 시간 (밀리초)
		private Long sessionEndTime;        // 세션 종료 시간 (밀리초)
		private Long durationSeconds;       // 총 운동 시간 (초)
		private Integer featureCount;       // Feature 개수 (10초 구간 개수)
		private Integer totalCoordinateCount; // 총 좌표 개수
		private Double avgPace;             // 평균 페이스 (km/h)
		private Integer avgBpm;             // 평균 심박수
		private Double maxHeight;           // 최고 고도 (m)
		private Double minHeight;           // 최저 고도 (m)
	}
}