package org.example.runningapp.oauth.dto;


public record KakaoUserInfo(
	String id,
	String email,
	String name,
	String imageUrl
) { }