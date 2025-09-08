package org.example.runningapp.common.exception;


public class OAuth2AuthenticationException extends RuntimeException {
	public OAuth2AuthenticationException(String message) {
		super(message);
	}
	public OAuth2AuthenticationException(String message, Throwable cause) {
		super(message, cause);
	}
}