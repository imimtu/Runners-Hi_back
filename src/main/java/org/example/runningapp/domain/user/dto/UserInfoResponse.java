package org.example.runningapp.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserInfoResponse(
	String username,
	String email
) {
}
