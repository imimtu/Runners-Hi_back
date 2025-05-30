package org.example.runningapp.data.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record GeoJsonFeatureCollection(
	@NotNull String type, // "FeatureCollection"
	@NotEmpty @Valid List<RunningFeature> features
) {}
