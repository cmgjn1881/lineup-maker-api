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

    @Column(nullable = true, length = 60)
    private String password;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE"; // ✨ [추가] 사용자 상태 필드

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Column(nullable = false, length = 20)
    private String provider = "local";

    @Column(name = "provider_id", nullable = true)
    private String providerId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder
    public Users(UUID userId, String email, String password, String username, String status, String provider, String providerId, Boolean isVerified) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.username = username;
        this.status = status != null ? status : "ACTIVE"; // ✨ [수정] Builder에 status 추가
        this.provider = provider != null ? provider : "local";
        this.providerId = providerId;
        this.isVerified = isVerified != null ? isVerified : false;
        this.createdAt = LocalDateTime.now();
    }

    public void updateUsername(String newUsername) {
        this.username = newUsername;
    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    public void completeVerification() {
        this.isVerified = true;
    }

    // ✨ [추가] 탈퇴 상태로 변경하는 메서드
    public void markAsWithdrawn() {
        this.status = "WITHDRAWN";
    }

    // ✨ [추가] 활성 상태로 변경하는 메서드 (재활성화 시)
    public void activate() {
        this.status = "ACTIVE";
    }
}
