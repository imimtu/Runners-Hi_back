package org.example.runningapp.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(
	@NotBlank(message = "토큰은 필수 입력값입니다.")
	String accessToken
) { }