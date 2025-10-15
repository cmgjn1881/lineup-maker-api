package com.lineupmaker.team.repository;

import com.lineupmaker.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, Long> {

    // 특정 owerId를 가진 모든 팀 목록을 조회하는 메서드
    List<Team> findByOwnerUserId(UUID ownerId);
}
