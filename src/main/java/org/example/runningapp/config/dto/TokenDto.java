package org.example.runningapp.config.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TokenDto(
	String accessToken,
	String refreshToken,
	Instant accessTokenExpiresAt,
	Instant refreshTokenExpiresAt
) { }