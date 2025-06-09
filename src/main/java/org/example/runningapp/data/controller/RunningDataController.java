package org.example.runningapp.data.controller;

import org.example.runningapp.data.dto.reqres.RunningDataRequest;
import org.example.runningapp.data.dto.reqres.RunningDataResponse;
import org.example.runningapp.data.entity.RunningSession;
import org.example.runningapp.data.service.RunningDataService;
import org.example.runningapp.oauth.UserPrincipal;
import org.springframework.http.HttpStatus;
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
	 * 러닝 데이터 저장 (GeoJSON 형태)
	 */
	@PostMapping("/session")
	public ResponseEntity<RunningDataResponse> saveRunningData(
		@Valid @RequestBody RunningDataRequest request,
		@AuthenticationPrincipal UserPrincipal currentUser) {

		// 첫 번째 Feature의 timestampStart를 로그에 사용
		Long firstTimestamp = request.geoData().features().get(0).properties().timestampStart();

		log.info("러닝 데이터 저장 요청 - 사용자: {}, 세션번호: {}, 첫번째타임스탬프: {}, Feature 수: {}, 총 좌표 수: {}",
			currentUser.getId(), request.sessionNum(), firstTimestamp,
			request.getFeatureCount(), request.getTotalCoordinateCount());

		// 러닝 데이터 저장
		RunningDataResponse response = runningDataService.saveRunningData(request, currentUser.getId());

		return "SUCCESS".equals(response.status()) ?
			ResponseEntity.ok(response) :
			ResponseEntity.badRequest().body(response);
	}

	/**
	 * 현재 사용자의 러닝 세션 목록 조회
	 */
	@GetMapping("/sessions")
	public ResponseEntity<List<RunningSession>> getMySessions(
		@RequestParam(defaultValue = "10") int limit,
		@AuthenticationPrincipal UserPrincipal currentUser) {

		log.info("사용자 러닝 세션 목록 조회 - 사용자: {}, 제한: {}", currentUser.getId(), limit);

		List<RunningSession> sessions = runningDataService.getUserSessions(currentUser.getId(), limit);
		return ResponseEntity.ok(sessions);
	}

	/**
	 * 특정 러닝 세션 데이터 조회
	 */
	@GetMapping("/session/{sessionId}")
	public ResponseEntity<RunningSession> getMySessionData(
		@PathVariable String sessionId,
		@AuthenticationPrincipal UserPrincipal currentUser) {

		log.info("사용자 러닝 세션 데이터 조회 - 사용자: {}, 세션: {}", currentUser.getId(), sessionId);

		RunningSession session = runningDataService.getSessionData(currentUser.getId(), sessionId);

		if (session == null) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(session);
	}

	/**
	 * 러닝 세션 요약 정보만 조회 (가벼운 요청)
	 */
	@GetMapping("/session/{sessionId}/summary")
	public ResponseEntity<RunningSession.SessionSummary> getSessionSummary(
		@PathVariable String sessionId,
		@AuthenticationPrincipal UserPrincipal currentUser) {

		RunningSession session = runningDataService.getSessionData(currentUser.getId(), sessionId);

		if (session == null || session.getSummary() == null) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(session.getSummary());
	}
}