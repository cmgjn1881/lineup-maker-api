package com.lineupmaker.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class Users {

    @Id
    // [변경] PostgreSQL은 UUID 생성을 DB에 맡기거나 코드로 처리합니다.
    // UUID 생성을 위한 @GenericGenerator 설정 제거
    @GeneratedValue(strategy = GenerationType.UUID) // JPA 3.1부터 제공되는 UUID 생성 전략
    @Column(name = "user_id") // columnDefinition 제거 (기본 UUID 타입 사용)
    private UUID userId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String username;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false; // 기본값 FALSE로 설정

    // 💡 추가 1: 이메일 인증 토큰
    @Column(name = "email_check_token")
    private String emailCheckToken;

    // 💡 추가 2: 토큰 만료 시간
    @Column(name = "token_expiry_date")
    private LocalDateTime emailTokenExpiryDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public Users(String email, String password, String username) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.createdAt = LocalDateTime.now();
    }

    // --- 인증 로직을 위한 Setter 역할의 메서드 추가 ---
    /**
     * 이메일 인증 성공 시 상태를 업데이트합니다.
     */
    public void completeVerification() {
        this.isVerified = true;
        this.emailCheckToken = null; // 사용된 토큰은 즉시 무효화
        this.emailTokenExpiryDate = null; // 만료 시간 제거
    }
}
