package org.example.runningapp.oauth.dto;

import org.example.runningapp.User;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserDto(
	Long id,
	String username,
	String email,
	String profileImage
) {
	public static UserDto from(User user) {
		return new UserDto(
			user.getId(),
			user.getUsername(),
			user.getEmail(),
			user.getProfileImage()
		);
	}
}