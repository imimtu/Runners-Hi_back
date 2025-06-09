package org.example.runningapp.simpleLogin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SimpleLoginRequest(
	@NotBlank(message = "사용자명은 필수 입력값입니다.")
	@Size(min = 2, max = 20, message = "사용자명은 2-20자 사이여야 합니다.")
	String username,
	String email  //null 가능
) { }