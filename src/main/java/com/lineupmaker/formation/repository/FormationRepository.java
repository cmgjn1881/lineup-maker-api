package com.lineupmaker.formation.repository;

import com.lineupmaker.formation.entity.Formation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FormationRepository extends JpaRepository<Formation, Long> {

    List<Formation> findByUser_UserIdAndTeam_TeamId(UUID userId, Long teamId);
}
