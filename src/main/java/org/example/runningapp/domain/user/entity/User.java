package org.example.runningapp.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import org.example.runningapp.common.security.AuthProvider;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String username;

	@Column(unique = true)
	private String email;

	@Column(unique = true, name = "kakao_id")
	private String kakaoId;

	@Column(name = "profile_image")
	private String profileImage;

	@Enumerated(EnumType.STRING)
	private AuthProvider provider;

	@CreatedDate
	@Column(updatable = false, name = "created_at")
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	// 프로필 업데이트를 위한 메소드 (불변성 유지를 위해 제한된 접근)
	public User updateProfile(String username, String profileImage) {
		this.username = username;
		this.profileImage = profileImage;
		return this;
	}

	@Column(name = "refresh_token")
	private String refreshToken;

	public void updateRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
}