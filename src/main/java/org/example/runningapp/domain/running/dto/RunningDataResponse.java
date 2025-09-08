package org.example.runningapp.domain.running.dto;

import java.time.LocalDateTime;

public record RunningDataResponse(
	String status,
	String message,
	Integer savedFeatureCount,       // 저장된 Feature 개수
	Integer totalCoordinateCount,    // 총 좌표 개수
	LocalDateTime timestamp,

	// 요약 정보
	RunningSessionSummary summary
) {
	public static RunningDataResponse success(int featureCount, int coordinateCount, RunningSessionSummary summary) {
		return new RunningDataResponse(
			"SUCCESS",
			"러닝 데이터 저장 완료",
			featureCount,
			coordinateCount,
			LocalDateTime.now(),
			summary
		);
	}

	public static RunningDataResponse error(String message) {
		return new RunningDataResponse(
			"ERROR",
			message,
			null,
			null,
			LocalDateTime.now(),
			null
		);
	}
}