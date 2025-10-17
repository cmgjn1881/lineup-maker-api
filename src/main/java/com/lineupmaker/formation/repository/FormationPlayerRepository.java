package com.lineupmaker.formation.repository;

import com.lineupmaker.formation.entity.FormationPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FormationPlayerRepository extends JpaRepository<FormationPlayer, Long> {

    // 특정 Formation ID에 속한 모든 배치 정보를 조회하는 메서드
    List<FormationPlayer> findByFormationFormationId(Long formationId);
}
