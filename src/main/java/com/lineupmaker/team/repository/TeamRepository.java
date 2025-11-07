package com.lineupmaker.team.repository;

import com.lineupmaker.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, Long> {

    @Query("SELECT t FROM Team t JOIN FETCH t.owner WHERE t.owner.userId = :ownerId")
    List<Team> findByOwnerUserIdWithUser(@Param("ownerId") UUID ownerId);

    long countByOwnerUserId(UUID ownerId);

    @Query("SELECT t FROM Team t JOIN FETCH t.owner WHERE t.teamId = :teamId")
    Optional<Team> findByIdWithUser(@Param("teamId") Long teamId);
}
