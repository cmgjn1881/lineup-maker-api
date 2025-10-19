package com.lineupmaker.user.repository;

import com.lineupmaker.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<Users, UUID> {
    // 이메일로 사용자 정보를 찾는 기능 추가 (로그인 및 중복 확인에 사용)
    Optional<Users> findByEmail(String email);

    // 💡 인증 토큰으로 사용자 찾기 (인증 링크 클릭 시 사용)
    Optional<Users> findByEmailCheckToken(String emailCheckToken);
}
