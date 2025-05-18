package org.example.runningapp.config;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.example.runningapp.config.dto.TokenDto;
import org.example.runningapp.exception.InvalidJwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.security.SignatureException;
import java.time.Instant;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

	private final Key key;
	private final long accessTokenExpirationMs;
	private final long refreshTokenExpirationMs;

	public JwtTokenProvider(
		@Value("${app.auth.jwt.secret-key}") String secretKey,
		@Value("${app.auth.jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
		@Value("${app.auth.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
		// 보안을 위해 충분히 긴 키 사용 (256비트 이상)
		this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
		this.accessTokenExpirationMs = accessTokenExpirationMs;
		this.refreshTokenExpirationMs = refreshTokenExpirationMs;
	}

	public TokenDto generateTokenPair(Long userId) {
		Instant now = Instant.now();
		Instant accessTokenExpiry = now.plusMillis(accessTokenExpirationMs);
		Instant refreshTokenExpiry = now.plusMillis(refreshTokenExpirationMs);

		String accessToken = createToken(userId, accessTokenExpirationMs);
		String refreshToken = createToken(userId, refreshTokenExpirationMs);

		return new TokenDto(accessToken, refreshToken, accessTokenExpiry, refreshTokenExpiry);
	}

	private String createToken(Long userId, long expirationMs) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + expirationMs);

		return Jwts.builder()
			.setSubject(Long.toString(userId))
			.setIssuedAt(now)
			.setExpiration(expiryDate)
			.signWith(key, SignatureAlgorithm.HS512) // 더 강력한 알고리즘 사용
			.compact();
	}

	public Long getUserIdFromToken(String token) {
		Claims claims = parseToken(token);
		return Long.parseLong(claims.getSubject());
	}

	public Claims parseToken(String token) {
		try {
			return Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token)
				.getBody();
		} catch (ExpiredJwtException e) {
			throw new InvalidJwtException("만료된 JWT 토큰입니다.", e);
		} catch (UnsupportedJwtException e) {
			throw new InvalidJwtException("지원되지 않는 JWT 토큰입니다.", e);
		} catch (MalformedJwtException e) {
			throw new InvalidJwtException("유효하지 않은 JWT 서명입니다.", e);
		} catch (IllegalArgumentException e) {
			throw new InvalidJwtException("JWT 토큰이 비어있습니다.", e);
		} catch (Exception e) {
			throw new InvalidJwtException("JWT 토큰 검증 중 오류가 발생했습니다.", e);
		}
	}

	public boolean validateToken(String token) {
		try {
			parseToken(token);
			return true;
		} catch (InvalidJwtException e) {
			log.debug("JWT 토큰 검증 실패: {}", e.getMessage());
			return false;
		}
	}
}