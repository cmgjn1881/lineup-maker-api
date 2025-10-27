package com.lineupmaker.user.repository;

import com.lineupmaker.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<Users, UUID> {

    // 이메일로 ACTIVE 상태의 사용자 조회
    Optional<Users> findByEmailAndStatus(String email, String status);

    // 이메일 중복 확인을 위해 모든 상태의 사용자 조회
    Optional<Users> findByEmail(String email);

    // 소셜 로그인을 위해 모든 상태의 사용자 조회 (탈퇴 후 재활성화 고려)
    Optional<Users> findByProviderAndProviderId(String provider, String providerId);

    // ID로 ACTIVE 상태의 사용자 조회
    @Query("SELECT u FROM Users u WHERE u.userId = :userId AND u.status = 'ACTIVE'")
    Optional<Users> findActiveById(@Param("userId") UUID userId);
}
