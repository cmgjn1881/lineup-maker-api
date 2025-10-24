package com.lineupmaker.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID userId;

    @Column(unique = true, nullable = true)
    private String email;

    @Column(nullable = true, length = 60) // length 지정 (60자)
    private String password;

    @Column(nullable = false, length = 50) // length 지정 (50자)
    private String username;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    // ⭐️ [수정]: provider 필드에 기본값 설정 (DB NOT NULL에 맞춤)
    // Spring이 기본값을 인식할 수 있도록 @Builder에서는 값을 명시적으로 받거나 필드 초기화를 사용합니다.
    @Column(nullable = false, length = 20)
    private String provider = "local"; // ⭐️ 필드 레벨에서 기본값 'local' 설정

    @Column(name = "provider_id", nullable = true)
    private String providerId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // ⭐️ 필드 레벨에서 기본값 설정

    // ⭐️ [핵심 수정]: Builder를 소셜 로그인과 일반 로그인 모두 지원하도록 확장
    @Builder
    public Users(UUID userId, String email, String password, String username, String provider, String providerId, Boolean isVerified) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.username = username;
        // 🚨 provider가 명시되지 않은 경우 'local'로 설정 (로컬 회원가입 시)
        this.provider = provider != null ? provider : "local";
        this.providerId = providerId;
        this.isVerified = isVerified != null ? isVerified : false;
        this.createdAt = LocalDateTime.now();
    }

    // 💡 [추가] 닉네임 업데이트 메서드 (소셜 로그인 재접속 시 사용)
    public void updateUsername(String newUsername) {
        this.username = newUsername;
    }

    public void completeVerification() {
        this.isVerified = true;
    }
}