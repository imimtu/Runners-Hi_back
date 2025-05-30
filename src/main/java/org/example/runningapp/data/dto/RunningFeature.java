package org.example.runningapp.data.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RunningFeature(
	@NotNull String type, // "Feature"
	@Valid RunningProperties properties,
	@Valid RunningGeometry geometry
) {}
