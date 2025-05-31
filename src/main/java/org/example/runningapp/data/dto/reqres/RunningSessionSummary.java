package org.example.runningapp.data.dto.reqres;

import org.example.runningapp.data.dto.RunningFeature;

public record RunningSessionSummary(
	Long sessionStartTime,    // 세션 시작 시간
	Long sessionEndTime,      // 세션 종료 시간
	Long durationSeconds,     // 총 운동 시간(초)
	Double totalDistanceKm,   // 총 거리(km) - 추후 계산
	Double avgPaceKmh,        // 평균 페이스(km/h)
	Integer avgBpm,           // 평균 심박수
	Double maxHeight,         // 최고 고도
	Double minHeight          // 최저 고도
) {
	public static RunningSessionSummary fromFeatures(java.util.List<RunningFeature> features) {
		if (features == null || features.isEmpty()) return null;

		// 시간 범위 계산
		Long startTime = features.stream()
			.mapToLong(f -> f.properties().timestampStart())
			.min().orElse(0L);

		Long endTime = features.stream()
			.mapToLong(f -> f.properties().timestampEnd())
			.max().orElse(0L);

		// 평균값 계산 (null 체크 포함)
		Double avgPace = features.stream()
			.map(f -> f.properties().pace())
			.filter(java.util.Objects::nonNull)
			.mapToDouble(Double::doubleValue)
			.average().orElse(0.0);

		Double avgBpmDouble = features.stream()
			.map(f -> f.properties().bpm())
			.filter(java.util.Objects::nonNull)
			.mapToInt(Integer::intValue)
			.average().orElse(0.0);

		// 고도 범위
		Double maxHeight = features.stream()
			.map(f -> f.properties().height())
			.filter(java.util.Objects::nonNull)
			.mapToDouble(Double::doubleValue)
			.max().orElse(0.0);

		Double minHeight = features.stream()
			.map(f -> f.properties().height())
			.filter(java.util.Objects::nonNull)
			.mapToDouble(Double::doubleValue)
			.min().orElse(0.0);

		return new RunningSessionSummary(
			startTime,
			endTime,
			(endTime - startTime) / 1000, // 초 단위
			0.0, // 거리는 추후 계산
			avgPace,
			(int) Math.round(avgBpmDouble),
			maxHeight,
			minHeight
		);
	}
}