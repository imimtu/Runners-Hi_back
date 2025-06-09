package org.example.runningapp.oauth.controller;

import java.util.Map;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.runningapp.User;
import org.example.runningapp.config.JwtTokenProvider;
import org.example.runningapp.config.redis.RedisService;

import org.example.runningapp.config.dto.TokenDto;
import org.example.runningapp.exception.InvalidJwtException;
import org.example.runningapp.oauth.dto.AuthResponse;
import org.example.runningapp.oauth.dto.KakaoLoginRequest;
import org.example.runningapp.oauth.dto.KakaoUserInfo;

import org.example.runningapp.oauth.dto.UserDto;
import org.example.runningapp.oauth.kakao.KakaoService;
import org.example.runningapp.simpleLogin.SimpleAuthService;
import org.example.runningapp.simpleLogin.SimpleLoginRequest;
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
	private final SimpleAuthService simpleAuthService;  // 새로 추가
	private final JwtTokenProvider tokenProvider;
	private final RedisService redisService;

	@PostMapping("/kakao/login")
	public ResponseEntity<AuthResponse> kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
		log.debug("카카오 로그인 요청 처리");

		// 1. 카카오 토큰으로 사용자 정보 조회
		KakaoUserInfo userInfo = kakaoService.getUserInfo(request.accessToken());

		// 2. 사용자 정보로 회원가입/로그인 처리
		User user = kakaoService.loginOrSignup(userInfo);

		// 3. JWT 토큰 생성
		TokenDto tokenDto = tokenProvider.generateTokenPair(user.getId());

		// 4. 리프레시 토큰 Redis에 저장
		redisService.saveRefreshToken(user.getId(), tokenDto.refreshToken());

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

		// 3. 리프레시 토큰 Redis에 저장
		redisService.saveRefreshToken(user.getId(), tokenDto.refreshToken());

		// 4. 응답 반환
		log.info("간단 로그인 성공 - 사용자 ID: {}, 사용자명: {}", user.getId(), user.getUsername());
		return ResponseEntity.ok(new AuthResponse(tokenDto, UserDto.from(user)));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@RequestBody Map<String, String> request) {
		String accessToken = request.get("accessToken");

		if (StringUtils.hasText(accessToken)) {
			try {
				// 1. 토큰에서 사용자 ID 추출
				Long userId = tokenProvider.getUserIdFromToken(accessToken);

				// 2. Redis에서 리프레시 토큰 삭제
				redisService.deleteRefreshToken(userId);

				// 3. 액세스 토큰을 블랙리스트에 추가
				Claims claims = tokenProvider.parseToken(accessToken);
				long expirationTime = claims.getExpiration().getTime() - System.currentTimeMillis();
				if (expirationTime > 0) {
					redisService.addToBlacklist(accessToken, expirationTime);
				}
			} catch (InvalidJwtException e) {
				log.debug("로그아웃 처리 중 무효한 토큰: {}", e.getMessage());
				// 이미 만료된 토큰이므로 추가 처리 필요 없음
			}
		}

		return ResponseEntity.ok().build();
	}
}