package com.lineupmaker.player.repository;

import com.lineupmaker.player.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    List<Player> findByTeamTeamId(Long teamId);
}
