package org.example.runningapp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByKakaoId(String kakaoId);
	Optional<User> findByEmail(String email);
	Optional<User> findByUsername(String username);
	boolean existsByEmail(String email);
	boolean existsByUsername(String username);
}