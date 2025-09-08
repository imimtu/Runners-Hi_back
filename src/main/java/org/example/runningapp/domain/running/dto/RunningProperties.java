package org.example.runningapp.domain.running.dto;

import jakarta.validation.constraints.NotNull;

public record RunningProperties(
	@NotNull Long timestampStart,     // 구간 시작 시각 (밀리초)
	@NotNull Long timestampEnd,       // 구간 종료 시각 (밀리초)

	// 필수 데이터 (있을 때만)
	Double height,                    // 고도(m)
	Integer bpm,                      // 심박수(bpm)
	Double pace,                      // 페이스(km/h)
	Integer power,                    // 파워(w)
	Integer cadence,                  // 케이던스(spm)

	// 고급 데이터 (대부분 0이 될 예정)
	Double minVerticalAmplitude,      // 최소 수직 높이(cm)
	Double maxVerticalAmplitude,      // 최대 수직 높이(cm)
	Integer minGct,                   // 최소 지면 접촉 시간(ms)
	Integer maxGct,                   // 최대 지면 접촉 시간(ms)
	Double stride                     // 보폭(m)
) {}