package org.example.runningapp.domain.auth.controller;

import java.util.Date;
import java.util.Map;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.runningapp.domain.auth.document.BlacklistedToken;
import org.example.runningapp.domain.auth.repository.BlacklistedTokenRepository;
import org.example.runningapp.domain.user.entity.User;
import org.example.runningapp.common.security.JwtTokenProvider;

import org.example.runningapp.config.dto.TokenDto;
import org.example.runningapp.common.exception.InvalidJwtException;
import org.example.runningapp.domain.auth.dto.AuthResponse;
import org.example.runningapp.domain.auth.dto.KakaoLoginRequest;
import org.example.runningapp.domain.auth.dto.KakaoUserInfo;

import org.example.runningapp.domain.auth.dto.UserDto;
import org.example.runningapp.domain.auth.service.KakaoService;
import org.example.runningapp.domain.auth.service.SimpleAuthService;
import org.example.runningapp.domain.auth.dto.SimpleLoginRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final KakaoService kakaoService;
	private final SimpleAuthService simpleAuthService;
	private final JwtTokenProvider tokenProvider;
	private final BlacklistedTokenRepository blacklistedTokenRepository;

	@PostMapping("/kakao/login")
	public ResponseEntity<AuthResponse> kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
		log.debug("카카오 로그인 요청 처리");

		// 1. 카카오 토큰으로 사용자 정보 조회
		KakaoUserInfo userInfo = kakaoService.getUserInfo(request.accessToken());

		// 2. 사용자 정보로 회원가입/로그인 처리
		User user = kakaoService.loginOrSignup(userInfo);

		// 3. JWT 토큰 생성
		TokenDto tokenDto = tokenProvider.generateTokenPair(user.getId());

		// 4. 리프레시 토큰 DB에 저장
		simpleAuthService.updateUserRefreshToken(user.getId(), tokenDto.refreshToken());

		// 5. 응답 반환
		return ResponseEntity.ok(new AuthResponse(tokenDto, UserDto.from(user)));
	}

	/**
	 * 간단 로그인/회원가입 (테스트용)
	 * username만으로 회원가입 또는 로그인 처리
	 */
	@PostMapping("/simple-login")
	public ResponseEntity<AuthResponse> simpleLogin(@Valid @RequestBody SimpleLoginRequest request) {
		log.debug("간단 로그인 요청 처리 - 사용자명: {}", request.username());

		// 1. 간단 로그인/회원가입 처리
		User user = simpleAuthService.simpleLoginOrSignup(request);

		// 2. JWT 토큰 생성
		TokenDto tokenDto = tokenProvider.generateTokenPair(user.getId());

		// 3. 리프레시 토큰 DB에 저장
		simpleAuthService.updateUserRefreshToken(user.getId(), tokenDto.refreshToken());

		// 4. 응답 반환
		log.info("간단 로그인 성공 - 사용자 ID: {}, 사용자명: {}", user.getId(), user.getUsername());
		return ResponseEntity.ok(new AuthResponse(tokenDto, UserDto.from(user)));
	}

    // TODO: 아래 케이스에 맞는 올바른 변수명으로 정정 필요.
    //  - login API : input의 'accessToken' 은 카카오 접근 토큰
    //  - logout API : input의 'accessToken' 은 서버가 발급한 JWT refresh 토큰
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@RequestBody Map<String, String> request) {
		String accessToken = request.get("accessToken");

		if (StringUtils.hasText(accessToken)) {
			try {
				// 1. 만료 여부와 상관없이 토큰에서 사용자 ID 추출
				Long userId = tokenProvider.getUserIdFromTokenIgnoringExpiration(accessToken);
                log.debug("userId : {}", userId);

				// 2. DB에서 리프레시 토큰 삭제 (핵심 로그아웃 로직)
				simpleAuthService.updateUserRefreshToken(userId, null);

				// 3. 아직 유효한 토큰이라면 블랙리스트에 추가하여 즉시 사용 불가 처리
				Claims claims = tokenProvider.parseToken(accessToken); // 유효성(만료 포함) 검사
				Date expiration = claims.getExpiration();

				BlacklistedToken blacklistedToken = new BlacklistedToken(null, accessToken, expiration);
				blacklistedTokenRepository.save(blacklistedToken);

			} catch (InvalidJwtException e) {
				// - 토큰이 이미 만료된 경우: parseToken에서 예외 발생. 하지만 리프레시 토큰은 이미 삭제되었으므로 로그아웃 처리 완료.
				// - 토큰 서명이 위조된 경우: getUserIdFromTokenIgnoringExpiration에서 예외 발생. 어차피 유효하지 않은 토큰이므로 무시.
				log.debug("로그아웃 처리 중 토큰 오류 발생 (만료 등 정상적인 경우 포함): {}", e.getMessage());
			}
		}

		return ResponseEntity.ok().build();
	}
}