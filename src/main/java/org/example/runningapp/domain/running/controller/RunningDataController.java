package org.example.runningapp.domain.running.controller;

import org.example.runningapp.domain.running.dto.RunningDataRequest;
import org.example.runningapp.domain.running.dto.RunningDataResponse;
import org.example.runningapp.domain.running.entity.RunningSession;
import org.example.runningapp.domain.running.service.RunningDataService;
import org.example.runningapp.common.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/running")
@RequiredArgsConstructor
public class RunningDataController {

	private final RunningDataService runningDataService;

	/**
	 * 다음 러닝 세션 번호 조회
	 */
	@GetMapping("/next-session")
	public ResponseEntity<Integer> getNextSessionNumber(
		@AuthenticationPrincipal UserPrincipal currentUser) {

		log.info("다음 세션 번호 조회 - 사용자: {}", currentUser.getId());

		Integer nextSessionNum = runningDataService.getNextSessionNumber(currentUser.getId());
		return ResponseEntity.ok(nextSessionNum);
	}


	/**
	 * 러닝 데이터 저장 - 핵심 기능
	 */
	@PostMapping("/session")
	public ResponseEntity<RunningDataResponse> saveRunningData(
		@Valid @RequestBody RunningDataRequest request,
		@AuthenticationPrincipal UserPrincipal currentUser) {

		log.info("러닝 데이터 저장 - 사용자: {}, 세션번호: {}, Feature 수: {}",
			currentUser.getId(), request.sessionNum(), request.getFeatureCount());

		RunningDataResponse response = runningDataService.saveRunningData(request, currentUser.getId());

		return "SUCCESS".equals(response.status()) ?
			ResponseEntity.ok(response) :
			ResponseEntity.badRequest().body(response);
	}

	/**
	 * 러닝 세션 목록 조회
	 */
	@GetMapping("/sessions")
	public ResponseEntity<List<RunningSession>> getMySessions(
		@RequestParam(defaultValue = "10") int limit,
		@AuthenticationPrincipal UserPrincipal currentUser) {

		List<RunningSession> sessions = runningDataService.getUserSessions(currentUser.getId(), limit);
		return ResponseEntity.ok(sessions);
	}

	/**
	 * 특정 러닝 세션 조회
	 */
	@GetMapping("/session/{sessionKey}")
	public ResponseEntity<RunningSession> getMySessionData(
		@PathVariable String sessionKey,
		@AuthenticationPrincipal UserPrincipal currentUser) {

		RunningSession session = runningDataService.getSessionByKey(currentUser.getId(), sessionKey);

		return session != null ?
			ResponseEntity.ok(session) :
			ResponseEntity.notFound().build();
	}
}