package com.lineupmaker.formation.repository;

import com.lineupmaker.formation.entity.Formation;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FormationRepository extends JpaRepository<Formation, Long> {

    List<Formation> findByUser_UserIdAndTeam_TeamId(UUID userId, Long teamId, Sort sort);

    long countByTeamTeamId(Long teamId);

    /**
     * [Fetch Join 적용]
     * ID로 포메이션을 조회할 때, 연관된 User와 Team 엔티티를 함께 조회합니다.
     * LazyInitializationException을 방지합니다.
     */
    @Query("SELECT f FROM Formation f JOIN FETCH f.user JOIN FETCH f.team WHERE f.formationId = :id")
    Optional<Formation> findByIdWithUserAndTeam(@Param("id") Long id);
}
