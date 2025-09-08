package org.example.runningapp.domain.running.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document(collection = "running_sessions")
@CompoundIndex(def = "{'userId': 1, 'sessionKey': 1}")  // sessionId → sessionKey로 변경
@CompoundIndex(def = "{'userId': 1, 'createdAt': -1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunningSession {

	@Id
	private String id;

	private Long userId;
	private String sessionKey;        // "userId-sessionNum" 형태
	private Integer sessionNum;       // 원본 세션 번호

	private LocalDateTime createdAt;  // 생성 시간만 유지 (조회 정렬용)

	// 모든 10초 단위 features가 여기에 누적됨
	// 리스트 null 방지를 위한 어노테이션 추가
	@Builder.Default
	private List<Map<String, Object>> geoDataFeatures = new ArrayList<>();

	// Feature 개수 반환 (조회용)
	public int getCurrentFeatureCount() {
		return this.geoDataFeatures != null ? this.geoDataFeatures.size() : 0;
	}
}