package org.example.runningapp.common.exception;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	// 보안 이벤트 전용 로거
	private static final Logger SECURITY_LOGGER = LoggerFactory.getLogger("SECURITY_EVENTS");

	// =========================== 인증/보안 예외 ===========================

	@ExceptionHandler(InvalidJwtException.class)
	public ResponseEntity<ErrorResponse> handleInvalidJwtException(
		InvalidJwtException e, HttpServletRequest request) {

		// 구조화된 보안 로깅
		Map<String, Object> securityEvent = createSecurityEventMap(request, e, "JWT_ERROR");
		SECURITY_LOGGER.warn(Markers.append("security_event", securityEvent),
			"JWT 토큰 오류 - IP: {}, URI: {}, 상세: {}",
			getClientIP(request), request.getRequestURI(), getJwtErrorDetail(e.getMessage()));

		return ResponseEntity
			.status(HttpStatus.UNAUTHORIZED)
			.body(ErrorResponse.detailed(
				"AUTH-001",
				"토큰이 유효하지 않습니다",
				getJwtErrorDetail(e.getMessage()),
				request.getRequestURI()
			));
	}

	@ExceptionHandler(OAuth2AuthenticationException.class)
	public ResponseEntity<ErrorResponse> handleOAuth2AuthenticationException(
		OAuth2AuthenticationException e, HttpServletRequest request) {

		Map<String, Object> securityEvent = createSecurityEventMap(request, e, "OAUTH2_ERROR");
		SECURITY_LOGGER.error(Markers.append("security_event", securityEvent),
			"OAuth2 인증 실패 - IP: {}, URI: {}, 메시지: {}",
			getClientIP(request), request.getRequestURI(), e.getMessage());

		return ResponseEntity
			.status(HttpStatus.UNAUTHORIZED)
			.body(ErrorResponse.detailed(
				"AUTH-002",
				"소셜 로그인 인증에 실패했습니다",
				"카카오 로그인을 다시 시도해주세요",
				request.getRequestURI()
			));
	}

	// =========================== 비즈니스 로직 예외 ===========================

	@ExceptionHandler(UserAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(
		UserAlreadyExistsException e, HttpServletRequest request) {

		Map<String, Object> businessEvent = createBusinessEventMap(request, e, "USER_DUPLICATE");
		log.warn(Markers.append("business_event", businessEvent),
			"사용자 중복 생성 시도 - IP: {}, URI: {}, 메시지: {}",
			getClientIP(request), request.getRequestURI(), e.getMessage());

		return ResponseEntity
			.status(HttpStatus.CONFLICT)
			.body(ErrorResponse.detailed(
				"USER-001",
				e.getMessage(),
				"다른 사용자명이나 이메일을 사용해주세요",
				request.getRequestURI()
			));
	}

	@ExceptionHandler(InvalidRunningDataException.class)
	public ResponseEntity<ErrorResponse> handleInvalidRunningDataException(
		InvalidRunningDataException e, HttpServletRequest request) {

		Map<String, Object> businessEvent = createBusinessEventMap(request, e, "INVALID_GPS_DATA");
		log.warn(Markers.append("business_event", businessEvent),
			"잘못된 러닝 데이터 - IP: {}, URI: {}, 메시지: {}",
			getClientIP(request), request.getRequestURI(), e.getMessage());

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(ErrorResponse.detailed(
				"RUNNING-002",
				e.getMessage(),
				"GPS 데이터 형식을 확인해주세요",
				request.getRequestURI()
			));
	}

	@ExceptionHandler(ExternalServiceException.class)
	public ResponseEntity<ErrorResponse> handleExternalServiceException(
		ExternalServiceException e, HttpServletRequest request) {

		Map<String, Object> systemEvent = createSystemEventMap(request, e, "EXTERNAL_SERVICE_ERROR");
		log.error(Markers.append("system_event", systemEvent),
			"외부 서비스 오류 - IP: {}, URI: {}, 메시지: {}, 원인: {}",
			getClientIP(request), request.getRequestURI(), e.getMessage(),
			e.getCause() != null ? e.getCause().getClass().getSimpleName() : "Unknown");

		return ResponseEntity
			.status(HttpStatus.SERVICE_UNAVAILABLE)
			.body(ErrorResponse.detailed(
				"EXT-001",
				e.getMessage(),
				"잠시 후 다시 시도해주세요",
				request.getRequestURI()
			));
	}

	// =========================== 데이터베이스 예외 ===========================

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
		DataIntegrityViolationException e, HttpServletRequest request) {

		Map<String, Object> dbEvent = createDatabaseEventMap(request, e, "CONSTRAINT_VIOLATION");
		log.error(Markers.append("database_event", dbEvent),
			"데이터 무결성 위반 - IP: {}, URI: {}, 제약조건: {}",
			getClientIP(request), request.getRequestURI(), getDatabaseConstraintDetail(e.getMessage()));

		return ResponseEntity
			.status(HttpStatus.CONFLICT)
			.body(ErrorResponse.detailed(
				"DB-001",
				"데이터 제약 조건 위반",
				getDatabaseConstraintDetail(e.getMessage()),
				request.getRequestURI()
			));
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<ErrorResponse> handleDataAccessException(
		DataAccessException e, HttpServletRequest request) {

		Map<String, Object> dbEvent = createDatabaseEventMap(request, e, "CONNECTION_ERROR");
		log.error(Markers.append("database_event", dbEvent),
			"데이터베이스 접근 오류 - IP: {}, URI: {}, 타입: {}, 메시지: {}",
			getClientIP(request), request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage());

		return ResponseEntity
			.status(HttpStatus.SERVICE_UNAVAILABLE)
			.body(ErrorResponse.detailed(
				"DB-002",
				"데이터베이스 연결 오류",
				"일시적인 서버 문제입니다. 잠시 후 다시 시도해주세요",
				request.getRequestURI()
			));
	}

	// =========================== 기본 예외 처리 ===========================

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnknownException(Exception e, HttpServletRequest request) {
		Map<String, Object> systemEvent = createSystemEventMap(request, e, "UNKNOWN_ERROR");
		log.error(Markers.append("system_event", systemEvent),
			"예상치 못한 서버 오류 - IP: {}, URI: {}, 타입: {}, 메시지: {}",
			getClientIP(request), request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage(), e);

		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ErrorResponse.detailed(
				"SERVER-001",
				"예상치 못한 서버 오류가 발생했습니다",
				"관리자에게 문의하세요",
				request.getRequestURI()
			));
	}

	// =========================== 구조화된 로그 생성 메소드들 ===========================

	private Map<String, Object> createSecurityEventMap(HttpServletRequest request, Exception e, String eventType) {
		Map<String, Object> event = new HashMap<>();
		event.put("event_type", eventType);
		event.put("client_ip", getClientIP(request));
		event.put("user_agent", request.getHeader("User-Agent"));
		event.put("request_uri", request.getRequestURI());
		event.put("request_method", request.getMethod());
		event.put("exception_type", e.getClass().getSimpleName());
		event.put("message", e.getMessage());
		event.put("timestamp", LocalDateTime.now().toString());
		event.put("session_id", request.getSession(false) != null ? request.getSession().getId() : null);
		return event;
	}

	private Map<String, Object> createBusinessEventMap(HttpServletRequest request, Exception e, String eventType) {
		Map<String, Object> event = new HashMap<>();
		event.put("event_type", eventType);
		event.put("client_ip", getClientIP(request));
		event.put("request_uri", request.getRequestURI());
		event.put("exception_type", e.getClass().getSimpleName());
		event.put("message", e.getMessage());
		event.put("timestamp", LocalDateTime.now().toString());
		return event;
	}

	private Map<String, Object> createSystemEventMap(HttpServletRequest request, Exception e, String eventType) {
		Map<String, Object> event = new HashMap<>();
		event.put("event_type", eventType);
		event.put("client_ip", getClientIP(request));
		event.put("request_uri", request.getRequestURI());
		event.put("exception_type", e.getClass().getSimpleName());
		event.put("message", e.getMessage());
		event.put("cause", e.getCause() != null ? e.getCause().getClass().getSimpleName() : null);
		event.put("stack_trace_length", e.getStackTrace().length);
		event.put("timestamp", LocalDateTime.now().toString());
		return event;
	}

	private Map<String, Object> createDatabaseEventMap(HttpServletRequest request, Exception e, String eventType) {
		Map<String, Object> event = new HashMap<>();
		event.put("event_type", eventType);
		event.put("client_ip", getClientIP(request));
		event.put("request_uri", request.getRequestURI());
		event.put("exception_type", e.getClass().getSimpleName());
		event.put("message", e.getMessage());
		event.put("sql_state", extractSQLState(e));
		event.put("timestamp", LocalDateTime.now().toString());
		return event;
	}

	private String extractSQLState(Exception e) {
		// SQLException에서 SQLState 추출 로직
		Throwable cause = e;
		while (cause != null) {
			if (cause instanceof java.sql.SQLException) {
				return ((java.sql.SQLException) cause).getSQLState();
			}
			cause = cause.getCause();
		}
		return null;
	}

	// =========================== 기존 유틸리티 메소드들 유지 ===========================

	private String getClientIP(HttpServletRequest request) {
		String xfHeader = request.getHeader("X-Forwarded-For");
		if (xfHeader != null && !xfHeader.isEmpty() && !"unknown".equalsIgnoreCase(xfHeader)) {
			return xfHeader.split(",")[0].trim();
		}
		String xrHeader = request.getHeader("X-Real-IP");
		if (xrHeader != null && !xrHeader.isEmpty() && !"unknown".equalsIgnoreCase(xrHeader)) {
			return xrHeader;
		}
		return request.getRemoteAddr();
	}

	private String getJwtErrorDetail(String originalMessage) {
		if (originalMessage == null) {
			return "토큰 검증에 실패했습니다";
		}

		String lowerMessage = originalMessage.toLowerCase();

		if (lowerMessage.contains("expired")) {
			return "토큰이 만료되었습니다. 다시 로그인해주세요";
		} else if (lowerMessage.contains("malformed") || lowerMessage.contains("invalid")) {
			return "토큰 형식이 올바르지 않습니다";
		} else if (lowerMessage.contains("signature")) {
			return "토큰 서명이 유효하지 않습니다";
		} else if (lowerMessage.contains("unsupported")) {
			return "지원하지 않는 토큰 형식입니다";
		} else if (lowerMessage.contains("empty") || lowerMessage.contains("null")) {
			return "토큰이 제공되지 않았습니다";
		} else {
			return "토큰 검증에 실패했습니다";
		}
	}

	private String getDatabaseConstraintDetail(String originalMessage) {
		if (originalMessage == null) {
			return "데이터 제약 조건을 위반했습니다";
		}

		String lowerMessage = originalMessage.toLowerCase();

		if (lowerMessage.contains("unique") || lowerMessage.contains("duplicate")) {
			if (lowerMessage.contains("email")) {
				return "이미 사용 중인 이메일 주소입니다";
			} else if (lowerMessage.contains("username")) {
				return "이미 사용 중인 사용자명입니다";
			} else {
				return "중복된 데이터입니다";
			}
		} else if (lowerMessage.contains("foreign key")) {
			return "참조하는 데이터가 존재하지 않습니다";
		} else if (lowerMessage.contains("not null")) {
			return "필수 정보가 누락되었습니다";
		} else {
			return "데이터 형식이 올바르지 않습니다";
		}
	}

	// =========================== 에러 응답 DTO ===========================

	public record ErrorResponse(
		String code,
		String message,
		String detail,
		String path,
		LocalDateTime timestamp
	) {
		public static ErrorResponse detailed(String code, String message, String detail, String path) {
			return new ErrorResponse(code, message, detail, path, LocalDateTime.now());
		}
	}
}