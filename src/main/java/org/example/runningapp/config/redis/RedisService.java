package org.example.runningapp.config.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {

	private final RedisTemplate<String, String> redisTemplate;

	@Value("${app.auth.jwt.refresh-token-expiration-ms}")
	private long refreshTokenExpirationMs;

	// 리프레시 토큰 저장 (사용자 ID 기반)
	public void saveRefreshToken(Long userId, String refreshToken) {
		String key = getRefreshTokenKey(userId);
		redisTemplate.opsForValue().set(key, refreshToken, refreshTokenExpirationMs, TimeUnit.MILLISECONDS);
	}

	// 리프레시 토큰 조회
	public String getRefreshToken(Long userId) {
		String key = getRefreshTokenKey(userId);
		return redisTemplate.opsForValue().get(key);
	}

	// 리프레시 토큰 삭제 (로그아웃 시)
	public void deleteRefreshToken(Long userId) {
		String key = getRefreshTokenKey(userId);
		redisTemplate.delete(key);
	}

	// 액세스 토큰 블랙리스트 추가 (로그아웃 시)
	public void addToBlacklist(String token, long expiration) {
		String key = getBlacklistKey(token);
		redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofMillis(expiration));
	}

	// 토큰이 블랙리스트에 있는지 확인
	public boolean isTokenBlacklisted(String token) {
		String key = getBlacklistKey(token);
		return Boolean.TRUE.equals(redisTemplate.hasKey(key));
	}

	// 키 생성 메소드들 (접두사 사용하여 키 충돌 방지)
	private String getRefreshTokenKey(Long userId) {
		return "refresh:token:" + userId;
	}

	private String getBlacklistKey(String token) {
		return "blacklist:token:" + token;
	}
}