package org.example.runningapp.domain.running.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RunningFeature(
	@NotNull String type, // "Feature"
	@Valid RunningProperties properties,
	@Valid RunningGeometry geometry
) {}
