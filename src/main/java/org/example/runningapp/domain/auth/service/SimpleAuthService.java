package org.example.runningapp.domain.auth.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.runningapp.common.exception.UserAlreadyExistsException;
import org.example.runningapp.common.exception.ExternalServiceException;
import org.example.runningapp.domain.user.entity.User;
import org.example.runningapp.domain.user.repository.UserRepository;
import org.example.runningapp.common.security.AuthProvider;
import org.example.runningapp.domain.auth.dto.SimpleLoginRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleAuthService {

	private final UserRepository userRepository;

	@Transactional
	public User simpleLoginOrSignup(SimpleLoginRequest request) {
		log.debug("간단 로그인 시도 - 사용자명: {}", request.username());

		// 1. 입력값 추가 검증 (Controller 검증과 별개로)
		validateLoginRequest(request);

		// 2. username으로 기존 사용자 찾기
		Optional<User> existingUser = userRepository.findByUsername(request.username());

		if (existingUser.isPresent()) {
			// 기존 사용자 - 로그인 처리
			return handleExistingUser(existingUser.get(), request);
		} else {
			// 신규 사용자 - 회원가입 처리
			return handleNewUser(request);
		}
	}

	private void validateLoginRequest(SimpleLoginRequest request) {
		// 비즈니스 규칙 검증
		if (request.username().toLowerCase().contains("admin")) {
			throw new IllegalArgumentException("'admin'이 포함된 사용자명은 사용할 수 없습니다");
		}

		if (request.email() != null && !isValidEmailDomain(request.email())) {
			throw new IllegalArgumentException("허용되지 않은 이메일 도메인입니다");
		}
	}

	private User handleExistingUser(User existingUser, SimpleLoginRequest request) {
		log.info("기존 사용자 로그인 - ID: {}, 사용자명: {}",
			existingUser.getId(), existingUser.getUsername());

		// 이메일 업데이트 (필요시)
		if (shouldUpdateEmail(existingUser, request.email())) {
			// 이메일 중복 체크
			if (userRepository.existsByEmail(request.email())) {
				throw new UserAlreadyExistsException(
					String.format("이메일 '%s'는 다른 계정에서 이미 사용 중입니다", request.email())
				);
			}
			existingUser = existingUser.updateProfile(existingUser.getUsername(), existingUser.getProfileImage());
		}

		return existingUser;
	}

	private User handleNewUser(SimpleLoginRequest request) {
		log.debug("신규 사용자 회원가입 처리");

		// 비즈니스 검증들
		validateNewUser(request);

		try {
			User newUser = User.builder()
				.username(request.username())
				.email(request.email())
				.provider(AuthProvider.SIMPLE)
				.profileImage(null)
				.kakaoId(null)
				.build();

			User savedUser = userRepository.save(newUser);
			log.info("신규 사용자 회원가입 완료 - ID: {}, 사용자명: {}",
				savedUser.getId(), savedUser.getUsername());

			return savedUser;

		} catch (Exception e) {
			log.error("사용자 생성 실패 - 사용자명: {}, 오류: {}", request.username(), e.getMessage());
			throw new ExternalServiceException("사용자 계정 생성 중 오류가 발생했습니다", e);
		}
	}

	private void validateNewUser(SimpleLoginRequest request) {
		// 사용자명 중복 체크 (double-check)
		if (userRepository.existsByUsername(request.username())) {
			throw new UserAlreadyExistsException(
				String.format("사용자명 '%s'는 이미 사용 중입니다", request.username())
			);
		}

		// 이메일 중복 체크
		if (request.email() != null && userRepository.existsByEmail(request.email())) {
			throw new UserAlreadyExistsException(
				String.format("이메일 '%s'는 이미 등록된 계정입니다", request.email())
			);
		}

		// 비즈니스 규칙 검증
		if (isReservedUsername(request.username())) {
			throw new IllegalArgumentException("예약된 사용자명은 사용할 수 없습니다");
		}
	}

	private boolean shouldUpdateEmail(User user, String newEmail) {
		return newEmail != null && !newEmail.equals(user.getEmail());
	}

	private boolean isValidEmailDomain(String email) {
		// 허용된 도메인 체크 로직
		String[] allowedDomains = {"gmail.com", "naver.com", "kakao.com", "daum.net"};
		String domain = email.substring(email.lastIndexOf("@") + 1).toLowerCase();
		return java.util.Arrays.asList(allowedDomains).contains(domain);
	}

	private boolean isReservedUsername(String username) {
		String[] reserved = {"admin", "root", "system", "test", "guest"};
		return java.util.Arrays.asList(reserved).contains(username.toLowerCase());
	}
}
