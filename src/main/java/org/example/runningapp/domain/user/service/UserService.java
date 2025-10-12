package org.example.runningapp.domain.user.service;

import java.util.List;

import org.example.runningapp.common.exception.UserNotFoundException;
import org.example.runningapp.domain.running.repository.RunningSessionRepository;
import org.example.runningapp.domain.user.entity.User;
import org.example.runningapp.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
	private final UserRepository userRepository;
	private final RunningSessionRepository runningSessionRepository;

	public UserService(UserRepository userRepository,RunningSessionRepository runningSessionRepository) {
		this.runningSessionRepository=runningSessionRepository;
		this.userRepository = userRepository;
	}

	public void deleteUser(Long userId) {
		if (!userRepository.existsById(userId)) {
			throw new UserNotFoundException("ID " + userId + "에 해당하는 사용자가 존재하지 않습니다");
		}
		runningSessionRepository.deleteByUserId(userId);
		userRepository.deleteById(userId);
	}
}