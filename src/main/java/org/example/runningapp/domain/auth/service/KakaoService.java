package org.example.runningapp.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.runningapp.common.exception.ExternalServiceException;
import org.example.runningapp.common.exception.UserAlreadyExistsException;
import org.example.runningapp.domain.user.entity.User;
import org.example.runningapp.domain.user.repository.UserRepository;
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
				// 원인(cause)이 없는 경우 - RuntimeException으로 감싸기
				RuntimeException noResponseException = new RuntimeException("카카오 API 응답이 null입니다");
				throw new ExternalServiceException("카카오 API로부터 응답을 받지 못했습니다", noResponseException);
			}

			return extractKakaoUserInfo(body);

		} catch (RestClientException e) {
			log.error("카카오 API 호출 실패 - 토큰: {}..., 오류: {}",
				accessToken.substring(0, Math.min(10, accessToken.length())), e.getMessage());
			throw new ExternalServiceException("카카오 서버와 통신 중 오류가 발생했습니다", e);

		} catch (ExternalServiceException e) {
			throw e; // 이미 감싸진 예외는 다시 감싸지 않음

		} catch (Exception e) {
			log.error("카카오 사용자 정보 처리 중 예상치 못한 오류: {}", e.getMessage());
			throw new ExternalServiceException("카카오 사용자 정보를 처리할 수 없습니다", e);
		}
	}

	/**
	 * 카카오 사용자 정보로 로그인 또는 회원가입 처리
	 */
	@Transactional
	public User loginOrSignup(KakaoUserInfo userInfo) {
		// 1. 입력 데이터 검증
		validateKakaoUserInfo(userInfo);

		Optional<User> existingUser = userRepository.findByKakaoId(userInfo.id());

		if (existingUser.isPresent()) {
			return handleExistingKakaoUser(existingUser.get(), userInfo);
		} else {
			return handleNewKakaoUser(userInfo);
		}
	}

	private void validateKakaoUserInfo(KakaoUserInfo userInfo) {
		if (userInfo.id() == null || userInfo.id().trim().isEmpty()) {
			IllegalArgumentException invalidIdException = new IllegalArgumentException("카카오 ID가 null이거나 비어있습니다");
			throw new ExternalServiceException("카카오 사용자 ID를 확인할 수 없습니다", invalidIdException);
		}

		if (userInfo.name() == null || userInfo.name().trim().isEmpty()) {
			IllegalArgumentException invalidNameException = new IllegalArgumentException("카카오 닉네임이 null이거나 비어있습니다");
			throw new ExternalServiceException("카카오 계정에서 닉네임 정보를 가져올 수 없습니다", invalidNameException);
		}
	}

	private User handleExistingKakaoUser(User existingUser, KakaoUserInfo userInfo) {
		log.info("기존 카카오 사용자 로그인 - ID: {}, 카카오ID: {}",
			existingUser.getId(), userInfo.id());

		try {
			// 프로필 정보 업데이트
			return existingUser.updateProfile(userInfo.name(), userInfo.imageUrl());
		} catch (Exception e) {
			log.error("카카오 사용자 프로필 업데이트 실패 - 사용자ID: {}, 오류: {}",
				existingUser.getId(), e.getMessage());
			throw new ExternalServiceException("사용자 프로필 업데이트 중 오류가 발생했습니다", e);
		}
	}

	private User handleNewKakaoUser(KakaoUserInfo userInfo) {
		log.debug("신규 카카오 사용자 회원가입 처리");

		// 이메일 중복 체크 (카카오 이메일이 이미 등록된 경우)
		if (userInfo.email() != null && userRepository.existsByEmail(userInfo.email())) {
			throw new UserAlreadyExistsException(
				String.format("이메일 '%s'로 이미 가입된 계정이 있습니다. 기존 계정으로 로그인해주세요",
					userInfo.email())
			);
		}

		try {
			User newUser = User.builder()
				.kakaoId(userInfo.id())
				.email(userInfo.email())
				.username(userInfo.name())
				.profileImage(userInfo.imageUrl())
				.provider(AuthProvider.KAKAO)
				.build();

			User savedUser = userRepository.save(newUser);
			log.info("신규 카카오 사용자 회원가입 완료 - ID: {}, 카카오ID: {}",
				savedUser.getId(), userInfo.id());

			return savedUser;

		} catch (Exception e) {
			log.error("카카오 사용자 생성 실패 - 카카오ID: {}, 오류: {}", userInfo.id(), e.getMessage());
			throw new ExternalServiceException("카카오 계정 연동 중 오류가 발생했습니다", e);
		}
	}

	/**
	 * 카카오 사용자 정보에서 필요한 데이터 추출
	 */
	@SuppressWarnings("unchecked")
	private KakaoUserInfo extractKakaoUserInfo(Map<String, Object> body) {
		try {
			String id = body.get("id").toString();
			Map<String, Object> properties = (Map<String, Object>) body.get("properties");
			Map<String, Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");

			String nickname = properties != null ? (String) properties.get("nickname") : null;
			String profileImage = properties != null ? (String) properties.get("profile_image") : null;
			String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

			if (id == null) {
				NullPointerException noIdException = new NullPointerException("카카오 응답에서 'id' 필드를 찾을 수 없습니다");
				throw new ExternalServiceException("카카오 사용자 ID를 찾을 수 없습니다", noIdException);
			}

			return new KakaoUserInfo(id, email, nickname, profileImage);

		} catch (ClassCastException e) {
			log.error("카카오 응답 데이터 형변환 오류: {}", e.getMessage());
			throw new ExternalServiceException("카카오 응답 데이터 형식이 예상과 다릅니다", e);

		} catch (ExternalServiceException e) {
			throw e; // 이미 감싸진 예외는 다시 감싸지 않음

		} catch (Exception e) {
			log.error("카카오 사용자 정보 파싱 중 예상치 못한 오류: {}", e.getMessage());
			throw new ExternalServiceException("카카오 사용자 정보를 처리할 수 없습니다", e);
		}
	}
}