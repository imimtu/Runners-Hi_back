package org.example.runningapp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.runningapp.User;
import org.example.runningapp.UserRepository;
import org.example.runningapp.config.redis.RedisService;
import org.example.runningapp.exception.InvalidJwtException;
import org.example.runningapp.oauth.UserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider tokenProvider;
	private final UserRepository userRepository;
	private final RedisService redisService;

	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		try {
			Optional<String> jwt = extractJwtFromRequest(request);

			jwt.ifPresent(token -> {
				// 토큰이 블랙리스트에 있는지 확인 (로그아웃된 토큰)
				if (redisService.isTokenBlacklisted(token)) {
					log.debug("블랙리스트에 등록된 토큰입니다");
					return;
				}

				if (tokenProvider.validateToken(token)) {
					Long userId = tokenProvider.getUserIdFromToken(token);
					userRepository.findById(userId)
						.ifPresent(user -> setAuthenticationContext(user, request));
				}
			});
		} catch (InvalidJwtException e) {
			log.error("JWT 인증 처리 중 오류 발생: {}", e.getMessage());
			// 인증 실패 시 SecurityContext는 비워진 상태로 유지됨
		}

		filterChain.doFilter(request, response);
	}

	private Optional<String> extractJwtFromRequest(HttpServletRequest request) {
		String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
			return Optional.of(bearerToken.substring(BEARER_PREFIX.length()));
		}
		return Optional.empty();
	}

	private void setAuthenticationContext(User user, HttpServletRequest request) {
		List<SimpleGrantedAuthority> authorities =
			Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

		UserPrincipal principal = new UserPrincipal(user.getId(), user.getEmail(), "", authorities);

		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
			principal, null, authorities);
		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

		SecurityContextHolder.getContext().setAuthentication(authentication);
	}
}