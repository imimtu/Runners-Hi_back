package org.example.runningapp.domain.auth.dto;


import org.example.runningapp.config.dto.TokenDto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
	TokenDto token,
	UserDto user
) { }