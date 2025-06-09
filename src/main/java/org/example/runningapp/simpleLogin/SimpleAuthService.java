package org.example.runningapp.simpleLogin;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.runningapp.User;
import org.example.runningapp.UserRepository;
import org.example.runningapp.oauth.AuthProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleAuthService {

	private final UserRepository userRepository;

	/**
	 * 간단 로그인: username으로 회원가입 또는 로그인 처리
	 *
	 * @param request username과 선택적 email
	 * @return 로그인/회원가입된 사용자
	 */
	@Transactional
	public User simpleLoginOrSignup(SimpleLoginRequest request) {
		log.debug("간단 로그인 시도 - 사용자명: {}", request.username());

		// 1. username으로 기존 사용자 찾기
		Optional<User> existingUser = userRepository.findByUsername(request.username());

		if (existingUser.isPresent()) {
			// 2-1. 기존 사용자 - 로그인 처리
			User user = existingUser.get();
			log.info("기존 사용자 로그인 - ID: {}, 사용자명: {}", user.getId(), user.getUsername());

			// 이메일이 제공되고 기존과 다르면 업데이트
			if (request.email() != null && !request.email().equals(user.getEmail())) {
				user = user.updateProfile(user.getUsername(), user.getProfileImage());
				// 이메일 업데이트는 별도 메소드 필요시 추가
				log.debug("사용자 정보 업데이트됨");
			}

			return user;
		} else {
			// 2-2. 신규 사용자 - 회원가입 처리
			User newUser = User.builder()
				.username(request.username())
				.email(request.email()) // null일 수 있음
				.provider(AuthProvider.SIMPLE)
				.profileImage(null) // 기본값
				.kakaoId(null) // SIMPLE 로그인에서는 null
				.build();

			User savedUser = userRepository.save(newUser);
			log.info("신규 사용자 회원가입 완료 - ID: {}, 사용자명: {}",
				savedUser.getId(), savedUser.getUsername());

			return savedUser;
		}
	}
}