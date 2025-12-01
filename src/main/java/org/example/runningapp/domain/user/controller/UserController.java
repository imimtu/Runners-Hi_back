package org.example.runningapp.domain.user.controller;

import java.util.List;
import java.util.Map;

import org.example.runningapp.common.security.UserPrincipal;
import org.example.runningapp.domain.user.dto.UserInfoResponse;
import org.example.runningapp.domain.user.repository.UserRepository;
import org.example.runningapp.domain.user.entity.User;
import org.example.runningapp.domain.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
	private final UserRepository userRepository;
	private final UserService userService;

	public UserController(UserRepository userRepository, UserService userService) {
		this.userRepository = userRepository;
		this.userService = userService;
	}

	@PostMapping
	public User createUser(@RequestBody User user) {
		return userRepository.save(user);
	}

	@GetMapping
	public List<User> getAllUsers() {
		return userRepository.findAll();
	}

	@GetMapping("/{userId}")
	public ResponseEntity<UserInfoResponse> getUserInfo(
		@PathVariable Long userId,
		@AuthenticationPrincipal UserPrincipal userPrincipal
	) {
		if (!userId.equals(userPrincipal.getId())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		UserInfoResponse userInfo = userService.getUserInfo(userId);
		return ResponseEntity.ok(userInfo);
	}

	@DeleteMapping("/delete/{userId}")
	public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long userId) {
		userService.deleteUser(userId);

		return ResponseEntity.ok(Map.of(
			"message", "회원이 삭제되었습니다",
			"deletedUserId", userId
		));
	}
}