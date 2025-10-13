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
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tokenId;

    @Column(name = "user_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID userId;

    @Column(name = "token_value", unique = true, nullable = false, length = 512)
    private String tokenValue;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Builder
    public RefreshToken(UUID userId, String tokenValue, LocalDateTime expiresAt) {
        this.userId = userId;
        this.tokenValue = tokenValue;
        this.expiresAt = expiresAt;
    }

    // 토큰 값 업데이트 메서드 (재발급 시 사용 가능)
    public void updateTokenValue(String newTokenValue, LocalDateTime newExpiresAt) {
        this.tokenValue = newTokenValue;
        this.expiresAt = newExpiresAt;
    }
}
