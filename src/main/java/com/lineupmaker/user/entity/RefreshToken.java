package com.lineupmaker.user.entity;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RedisHash("refreshToken")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshToken implements Serializable {

    // Redis의 key가 될 사용자 ID
    @Id
    private UUID userId;

    // Redis에 저장할 토큰 값
    private String refreshToken;

    // 이 필드에 설정된 초 단위 값만큼 저장된 후 Redis에서 자동으로 삭제됩니다. (TTL)
    // 리프레시 토큰 만료 시간과 일치하도록 설정 (예: 7일)
    @TimeToLive(unit = TimeUnit.SECONDS)
    private Long expiration;

    // 토큰 값 업데이트 메서드 (토큰 재발급 시 사용)
    public void updateTokenValue(String newTokenValue, Long newExpiration) {
        this.refreshToken = newTokenValue;
        this.expiration = newExpiration;
    }
}
