package org.example.runningapp.common.exception;


public class InvalidJwtException extends RuntimeException {
	public InvalidJwtException(String message) {
		super(message);
	}
	public InvalidJwtException(String message, Throwable cause) {
		super(message, cause);
	}
}