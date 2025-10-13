package com.lineupmaker.user.repository;

import com.lineupmaker.user.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // 사용자 ID로 리프레시 토큰을 찾거나
    Optional<RefreshToken> findByUserId(UUID userId);

    // 토큰 값 자체로 토큰을 찾는 메서드 (클라이언트에서 받은 토큰을 검증할 때 사용)
    Optional<RefreshToken> findByTokenValue(String tokenValue);
}
