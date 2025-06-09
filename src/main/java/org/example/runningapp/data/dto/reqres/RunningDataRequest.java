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
	Integer sessionNum,
	@Valid GeoJsonFeatureCollection geoData
) {


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