package com.lineupmaker.user.repository;

import com.lineupmaker.user.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, UUID> {

    // 토큰 값 자체로 토큰을 찾는 메서드 (재발급 검증 시 필요)
    Optional<RefreshToken> findByRefreshToken(String refreshToken);
}
