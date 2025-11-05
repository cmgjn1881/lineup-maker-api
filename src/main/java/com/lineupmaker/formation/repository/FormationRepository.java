package com.lineupmaker.formation.repository;

import com.lineupmaker.formation.entity.Formation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FormationRepository extends JpaRepository<Formation, Long> {

    List<Formation> findByUser_UserIdAndTeam_TeamId(UUID userId, Long teamId);

    // 특정 팀에 속한 포메이션의 수를 세는 메서드
    long countByTeamTeamId(Long teamId);
}
