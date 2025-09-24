package org.example.runningapp.domain.auth.service;

import org.example.runningapp.common.exception.ExternalServiceException;
import org.example.runningapp.domain.user.entity.User;
import org.example.runningapp.domain.user.repository.UserRepository;
import org.example.runningapp.common.exception.OAuth2AuthenticationException;
import org.example.runningapp.common.security.AuthProvider;
import org.example.runningapp.domain.auth.dto.KakaoUserInfo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KakaoServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private RestTemplate restTemplate;

	@InjectMocks
	private KakaoService kakaoService;

	@Test
	void should_ReturnKakaoUserInfo_When_ValidAccessToken() {
		// given
		String accessToken = "valid-kakao-token";
		Map<String, Object> mockKakaoResponse = createMockKakaoApiResponse();

		when(restTemplate.exchange(
			eq("https://kapi.kakao.com/v2/user/me"),
			eq(HttpMethod.GET),
			any(HttpEntity.class),
			eq(Map.class)
		)).thenReturn(new ResponseEntity<>(mockKakaoResponse, HttpStatus.OK));

		// when
		KakaoUserInfo result = kakaoService.getUserInfo(accessToken);

		// then
		assertThat(result.id()).isEqualTo("12345");
		assertThat(result.name()).isEqualTo("테스트사용자");
		assertThat(result.email()).isEqualTo("test@kakao.com");
		assertThat(result.imageUrl()).isEqualTo("http://img.kakao.com/profile.jpg");
	}

	@Test
	void should_ThrowException_When_KakaoApiCallFails() {
		// given
		String accessToken = "invalid-token";

		when(restTemplate.exchange(
			anyString(),
			eq(HttpMethod.GET),
			any(HttpEntity.class),
			eq(Map.class)
		)).thenThrow(new RestClientException("API call failed"));

		// when & then
		assertThatThrownBy(() -> kakaoService.getUserInfo(accessToken))
			.isInstanceOf(ExternalServiceException.class)
			.hasMessage("카카오 서버와 통신 중 오류가 발생했습니다");
	}

	@Test
	void should_CreateNewUser_When_FirstTimeLogin() {
		// given
		KakaoUserInfo kakaoUserInfo = new KakaoUserInfo("12345", "test@kakao.com", "테스트사용자", "profile.jpg");

		when(userRepository.findByKakaoId("12345"))
			.thenReturn(Optional.empty());

		User savedUser = createTestUser();
		when(userRepository.save(any(User.class)))
			.thenReturn(savedUser);

		// when
		User result = kakaoService.loginOrSignup(kakaoUserInfo);

		// then
		assertThat(result.getKakaoId()).isEqualTo("12345");
		assertThat(result.getUsername()).isEqualTo("테스트사용자");
		assertThat(result.getEmail()).isEqualTo("test@kakao.com");
		assertThat(result.getProvider()).isEqualTo(AuthProvider.KAKAO);
	}

	@Test
	void should_UpdateExistingUser_When_UserAlreadyExists() {
		// given
		KakaoUserInfo kakaoUserInfo = new KakaoUserInfo("12345", "test@kakao.com", "업데이트된이름", "new-profile.jpg");

		User existingUser = createExistingTestUser();
		when(userRepository.findByKakaoId("12345"))
			.thenReturn(Optional.of(existingUser));

		// when
		User result = kakaoService.loginOrSignup(kakaoUserInfo);

		// then
		assertThat(result.getUsername()).isEqualTo("업데이트된이름");
		assertThat(result.getProfileImage()).isEqualTo("new-profile.jpg");
	}

	private Map<String, Object> createMockKakaoApiResponse() {
		return Map.of(
			"id", "12345",
			"properties", Map.of(
				"nickname", "테스트사용자",
				"profile_image", "http://img.kakao.com/profile.jpg"
			),
			"kakao_account", Map.of(
				"email", "test@kakao.com"
			)
		);
	}

	private User createTestUser() {
		return User.builder()
			.id(1L)
			.kakaoId("12345")
			.username("테스트사용자")
			.email("test@kakao.com")
			.profileImage("profile.jpg")
			.provider(AuthProvider.KAKAO)
			.createdAt(LocalDateTime.now())
			.build();
	}

	private User createExistingTestUser() {
		return User.builder()
			.id(1L)
			.kakaoId("12345")
			.username("기존사용자")
			.email("test@kakao.com")
			.profileImage("old-profile.jpg")
			.provider(AuthProvider.KAKAO)
			.createdAt(LocalDateTime.now().minusDays(1))
			.build();
	}
}