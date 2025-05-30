package org.example.runningapp.data.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RunningGeometry(
	@NotNull String type,                    // "LineString"
	@NotEmpty List<List<Double>> coordinates // [[경도, 위도], [경도, 위도], ...]
) {
	// 편의 메소드: 첫 번째 좌표
	public List<Double> getFirstCoordinate() {
		return coordinates.isEmpty() ? null : coordinates.get(0);
	}

	// 편의 메소드: 마지막 좌표
	public List<Double> getLastCoordinate() {
		return coordinates.isEmpty() ? null : coordinates.get(coordinates.size() - 1);
	}

	// 편의 메소드: 좌표 개수
	public int getCoordinateCount() {
		return coordinates.size();
	}
}