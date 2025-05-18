package org.example.runningapp.oauth.kakao;


import java.util.Map;

import org.example.runningapp.oauth.OAuth2UserInfo;

public class KakaoOAuth2UserInfo extends OAuth2UserInfo {

	private Map<String, Object> kakaoAccount;
	private Map<String, Object> profile;

	public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
		super(attributes);
		this.kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
		this.profile = kakaoAccount != null ?
			(Map<String, Object>) kakaoAccount.get("profile") : null;
	}

	@Override
	public String getId() {
		return attributes.get("id").toString();
	}

	@Override
	public String getName() {
		if (profile == null) {
			return null;
		}
		return (String) profile.get("nickname");
	}

	@Override
	public String getEmail() {
		if (kakaoAccount == null) {
			return null;
		}
		return (String) kakaoAccount.get("email");
	}

	@Override
	public String getImageUrl() {
		if (profile == null) {
			return null;
		}
		return (String) profile.get("profile_image_url");
	}
}