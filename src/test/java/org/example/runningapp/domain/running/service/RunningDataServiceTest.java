package org.example.runningapp.domain.running.service;

import org.example.runningapp.domain.running.dto.RunningDataRequest;
import org.example.runningapp.domain.running.dto.RunningDataResponse;
import org.example.runningapp.domain.running.dto.GeoJsonFeatureCollection;
import org.example.runningapp.domain.running.dto.RunningFeature;
import org.example.runningapp.domain.running.dto.RunningProperties;
import org.example.runningapp.domain.running.dto.RunningGeometry;
import org.example.runningapp.domain.running.entity.RunningSession;
import org.example.runningapp.domain.running.repository.RunningSessionRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RunningDataServiceTest {

	@Mock
	private RunningSessionRepository repository;

	@Mock
	private MongoTemplate mongoTemplate;

	@InjectMocks
	private RunningDataService runningDataService;

	@Test
	void should_CreateNewSession_When_NoExistingSessionFound() {
		// given
		Long userId = 1L;
		RunningDataRequest request = createTestRunningDataRequest();

		// Mock 설정: 기존 세션 없음
		when(repository.findByUserIdAndSessionKey(userId, "1-1"))
			.thenReturn(Optional.empty());

		// Mock 설정: save() 결과
		RunningSession savedSession = createTestRunningSession();
		when(repository.save(any(RunningSession.class)))
			.thenReturn(savedSession);

		// when
		RunningDataResponse result = runningDataService.saveRunningData(request, userId);

		// then
		assertThat(result.status()).isEqualTo("SUCCESS");
	}

	private RunningDataRequest createTestRunningDataRequest() {
		RunningGeometry geometry = new RunningGeometry("LineString",
			List.of(List.of(126.978266, 37.566733)));

		RunningProperties properties = new RunningProperties(
			System.currentTimeMillis() - 10000,
			System.currentTimeMillis(),
			50.0, 150, 5.5, null, null, null, null, null, null, null
		);

		RunningFeature feature = new RunningFeature("Feature", properties, geometry);
		GeoJsonFeatureCollection geoData = new GeoJsonFeatureCollection("FeatureCollection", List.of(feature));

		return new RunningDataRequest(1, geoData);
	}

	@Test
	void should_AppendToExistingSession_When_SessionAlreadyExists() {
		// given
		Long userId = 1L;
		RunningDataRequest request = createTestRunningDataRequest();

		// Mock 설정: 기존 세션이 존재함
		RunningSession existingSession = createExistingRunningSession();
		when(repository.findByUserIdAndSessionKey(userId, "1-1"))
			.thenReturn(Optional.of(existingSession));  // 첫 번째와의 차이점!

		// when
		RunningDataResponse result = runningDataService.saveRunningData(request, userId);

		// then
		assertThat(result.status()).isEqualTo("SUCCESS");

		// MongoTemplate의 updateFirst가 호출되었는지 검증 (선택사항)
		verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(RunningSession.class));
	}

	private RunningSession createExistingRunningSession() {
		return RunningSession.builder()
			.id("existing-session-id")
			.userId(1L)
			.sessionKey("1-1")
			.sessionNum(1)
			.createdAt(LocalDateTime.now().minusMinutes(1))  // 1분 전에 생성됨
			.geoDataFeatures(new ArrayList<>())  // 이미 일부 데이터가 있는 상태
			.build();
	}

	private RunningSession createTestRunningSession() {
		return RunningSession.builder()
			.id("test-id")
			.userId(1L)
			.sessionKey("1-1")
			.sessionNum(1)
			.createdAt(LocalDateTime.now())
			.build();
	}
}