package org.example.runningapp.domain.auth.dto;


public record KakaoUserInfo(
	String id,
	String email,
	String name,
	String imageUrl
) { }