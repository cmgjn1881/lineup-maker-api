package com.lineupmaker.player.repository;

import com.lineupmaker.player.entity.Player;
import com.lineupmaker.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    @Query("SELECT p FROM Player p JOIN FETCH p.team WHERE p.team.teamId = :teamId")
    List<Player> findByTeamTeamIdWithTeam(@Param("teamId") Long teamId);

    Optional<Player> findByTeamAndBackNumber(Team team, int backNumber);

    long countByTeamTeamId(Long teamId);

    @Query("SELECT p FROM Player p JOIN FETCH p.team WHERE p.playerId = :playerId")
    Optional<Player> findByIdWithTeam(@Param("playerId") Long playerId);
}
