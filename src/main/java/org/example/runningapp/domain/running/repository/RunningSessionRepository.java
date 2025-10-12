package org.example.runningapp.domain.running.repository;

import org.example.runningapp.domain.running.entity.RunningSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RunningSessionRepository extends MongoRepository<RunningSession, String> {

	// 간단한 조회들은 JPA로
	Optional<RunningSession> findTopByUserIdOrderBySessionNumDesc(Long userId);
	Optional<RunningSession> findByUserIdAndSessionKey(Long userId, String sessionKey);
	List<RunningSession> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	// 성능 최적화: sessionNum만 조회
	@Query(value = "{'userId': ?0}", fields = "{'sessionNum': 1}")
	Optional<RunningSession> findSessionNumOnlyByUserId(Long userId);
	void deleteByUserId(Long userId);

}