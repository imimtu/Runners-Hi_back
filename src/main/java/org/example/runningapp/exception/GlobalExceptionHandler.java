package org.example.runningapp.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(InvalidJwtException.class)
	public ResponseEntity<ErrorResponse> handleInvalidJwtException(InvalidJwtException e) {
		log.error("JWT 토큰 오류: {}", e.getMessage());
		return ResponseEntity
			.status(HttpStatus.UNAUTHORIZED)
			.body(new ErrorResponse("JWT 토큰 오류", e.getMessage()));
	}

	@ExceptionHandler(OAuth2AuthenticationException.class)
	public ResponseEntity<ErrorResponse> handleOAuth2AuthenticationException(OAuth2AuthenticationException e) {
		log.error("OAuth2 인증 오류: {}", e.getMessage());
		return ResponseEntity
			.status(HttpStatus.UNAUTHORIZED)
			.body(new ErrorResponse("OAuth2 인증 오류", e.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e) {
		BindingResult bindingResult = e.getBindingResult();
		String errorMessage = bindingResult.getFieldErrors().get(0).getDefaultMessage();

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(new ErrorResponse("VALID-001", errorMessage));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnknownException(Exception e) {
		log.error("서버 오류 발생: {}", e.getMessage(), e);
		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(new ErrorResponse("SERVER-001", "서버 오류가 발생했습니다."));
	}

	// 에러 응답 레코드
	public record ErrorResponse(String code, String message) {}
}