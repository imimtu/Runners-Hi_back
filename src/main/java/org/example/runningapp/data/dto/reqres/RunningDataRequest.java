package org.example.runningapp.data.dto.reqres;

import org.example.runningapp.data.dto.GeoJsonFeatureCollection;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RunningDataRequest(
	@NotNull Long userId,                    // 사용자 ID (보안 검증용)
	@NotBlank String sessionId,              // 러닝 세션 ID
	@Valid GeoJsonFeatureCollection geoData  // GeoJSON 데이터
) {
	// 편의 메소드들
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