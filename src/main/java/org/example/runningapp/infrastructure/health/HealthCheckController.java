package org.example.runningapp.infrastructure.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {
	@GetMapping("/health")
	public String healthCheck() {
		return "Running backend is up and running!";
	}
}