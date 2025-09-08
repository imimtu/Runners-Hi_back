package org.example.runningapp.domain.user.controller;

import java.util.List;

import org.example.runningapp.domain.user.repository.UserRepository;
import org.example.runningapp.domain.user.entity.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
	private final UserRepository userRepository;

	public UserController(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@PostMapping
	public User createUser(@RequestBody User user) {
		return userRepository.save(user);
	}

	@GetMapping
	public List<User> getAllUsers() {
		return userRepository.findAll();
	}
}