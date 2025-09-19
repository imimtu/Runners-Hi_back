package org.example.runningapp.common.exception;


public class InvalidRunningDataException extends RuntimeException {
	public InvalidRunningDataException(String message) {
		super(message);
	}

	public InvalidRunningDataException(String message, Throwable cause) {
		super(message, cause);
	}
}