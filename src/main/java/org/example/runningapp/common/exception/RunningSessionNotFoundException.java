package org.example.runningapp.common.exception;

public class RunningSessionNotFoundException extends RuntimeException {
	public RunningSessionNotFoundException(String message) {
		super(message);
	}
}
