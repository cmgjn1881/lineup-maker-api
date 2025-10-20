package com.lineupmaker.player.repository;

import com.lineupmaker.player.entity.Player;
import com.lineupmaker.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    List<Player> findByTeamTeamId(Long teamId);

    // 🔑 특정 팀에서 특정 등번호를 가진 선수를 찾는 메서드
    Optional<Player> findByTeamAndBackNumber(Team team, int backNumber);
}
