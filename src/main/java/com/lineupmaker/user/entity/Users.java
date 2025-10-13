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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public Users(String email, String password, String username) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.createdAt = LocalDateTime.now();
    }
}
