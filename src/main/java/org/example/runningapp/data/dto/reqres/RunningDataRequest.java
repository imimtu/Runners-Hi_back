package org.example.runningapp.data.dto.reqres;

import org.example.runningapp.data.dto.GeoJsonFeatureCollection;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RunningDataRequest(
	@NotNull(message = "세션 번호는 필수입니다.")
	@Min(value = 1, message = "세션 번호는 1 이상이어야 합니다.")
	Integer sessionNum,              // 세션 번호 (1, 2, 3, ...)

	@NotNull(message = "시작 타임스탬프는 필수입니다.")
	Long startTimestamp,             // 시작 타임스탬프 (밀리초)

	@Valid GeoJsonFeatureCollection geoData  // GeoJSON 데이터
) {
	// 완전한 sessionId 생성 메소드 (userId 조합)
	public String buildFullSessionId(Long userId) {
		return userId + "-" + sessionNum + "-" + startTimestamp;
	}

	// 기존 편의 메소드들
	public int getFeatureCount() {
		return geoData != null && geoData.features() != null ?
			geoData.features().size() : 0;
	}

	public int getTotalCoordinateCount() {
		if (geoData == null || geoData.features() == null) return 0;

		return geoData.features().stream()
			.mapToInt(feature -> feature.geometry().getCoordinateCount())
			.sum();
	}
}