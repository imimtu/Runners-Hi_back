package org.example.runningapp.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.runningapp.domain.user.entity.User;
import org.example.runningapp.domain.user.repository.UserRepository;
import org.example.runningapp.common.exception.OAuth2AuthenticationException;
import org.example.runningapp.common.security.AuthProvider;
import org.example.runningapp.domain.auth.dto.KakaoUserInfo;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoService {

	private final UserRepository userRepository;
	private final RestTemplate restTemplate;

	private static final String KAKAO_USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

	/**
	 * 카카오 토큰으로 사용자 정보 조회
	 */
	public KakaoUserInfo getUserInfo(String accessToken) {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);

			HttpEntity<String> entity = new HttpEntity<>(headers);
			ResponseEntity<Map> response = restTemplate.exchange(
				KAKAO_USER_INFO_URI,
				HttpMethod.GET,
				entity,
				Map.class
			);

			Map<String, Object> body = response.getBody();
			if (body == null) {
				throw new OAuth2AuthenticationException("카카오 API 응답이 비어있습니다");
			}

			return extractKakaoUserInfo(body);
		} catch (RestClientException e) {
			log.error("카카오 API 호출 중 오류 발생: {}", e.getMessage());
			throw new OAuth2AuthenticationException("카카오 API 호출 중 오류가 발생했습니다", e);
		} catch (Exception e) {
			log.error("카카오 사용자 정보 처리 중 오류 발생: {}", e.getMessage());
			throw new OAuth2AuthenticationException("카카오 사용자 정보를 처리할 수 없습니다", e);
		}
	}

	/**
	 * 카카오 사용자 정보에서 필요한 데이터 추출
	 */
	@SuppressWarnings("unchecked")
	private KakaoUserInfo extractKakaoUserInfo(Map<String, Object> body) {
		String id = body.get("id").toString();

		Map<String, Object> properties = (Map<String, Object>) body.get("properties");
		Map<String, Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");

		String nickname = properties != null ? (String) properties.get("nickname") : null;
		String profileImage = properties != null ? (String) properties.get("profile_image") : null;
		String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

		if (id == null) {
			throw new OAuth2AuthenticationException("카카오 ID를 찾을 수 없습니다");
		}

		return new KakaoUserInfo(id, email, nickname, profileImage);
	}

	/**
	 * 카카오 사용자 정보로 로그인 또는 회원가입 처리
	 */
	@Transactional
	public User loginOrSignup(KakaoUserInfo userInfo) {
		Optional<User> existingUser = userRepository.findByKakaoId(userInfo.id());

		if (existingUser.isPresent()) {
			// 기존 사용자 - 프로필 정보 업데이트
			return existingUser.get().updateProfile(userInfo.name(), userInfo.imageUrl());
		} else {
			// 신규 사용자 - 회원가입
			User newUser = User.builder()
				.kakaoId(userInfo.id())
				.email(userInfo.email())
				.username(userInfo.name())
				.profileImage(userInfo.imageUrl())
				.provider(AuthProvider.KAKAO)
				.build();

			return userRepository.save(newUser);
		}
	}
}