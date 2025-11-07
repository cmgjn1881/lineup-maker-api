package com.lineupmaker.player.service;

import com.lineupmaker.player.dto.PlayerCreateRequest;
import com.lineupmaker.player.dto.PlayerUpdateRequest;
import com.lineupmaker.player.entity.Player;
import com.lineupmaker.player.repository.PlayerRepository;
import com.lineupmaker.team.entity.Team;
import com.lineupmaker.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerService {

    private static final int MAX_PLAYERS_PER_TEAM = 30;

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;

    @Transactional
    public Player createPlayer(Long teamId, PlayerCreateRequest request, UUID ownerId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 팀 ID입니다: " + teamId));

        if (!team.getOwner().getUserId().equals(ownerId)) {
            throw new IllegalArgumentException("이 팀에 선수를 추가할 권한이 없습니다. (팀 소유자만 가능)");
        }

        long currentPlayerCount = playerRepository.countByTeamTeamId(teamId);
        if (currentPlayerCount >= MAX_PLAYERS_PER_TEAM) {
            throw new IllegalStateException("선수는 팀당 최대 " + MAX_PLAYERS_PER_TEAM + "명까지 생성할 수 있습니다.");
        }

        playerRepository.findByTeamAndBackNumber(team, request.getBackNumber())
                .ifPresent(p -> {
                    throw new IllegalArgumentException("이미 사용중인 등번호입니다.");
                });

        Player newPlayer = Player.builder()
                .team(team)
                .name(request.getName())
                .position(request.getPosition())
                .backNumber(request.getBackNumber())
                .build();

        return playerRepository.save(newPlayer);
    }

    @Transactional
    public Player updatePlayer(Long playerId, PlayerUpdateRequest request, UUID ownerId) {

        Player player = playerRepository.findByIdWithTeam(playerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 선수 ID입니다: " + playerId));

        if (!player.getTeam().getOwner().getUserId().equals(ownerId)) {
            throw new IllegalArgumentException("이 선수를 수정할 권한이 없습니다. (팀 소유자만 가능)");
        }

        if (player.getBackNumber().equals(request.getBackNumber())) {
            player.updateDetails(request.getName(), request.getPosition(), request.getBackNumber());
            return player;
        }

        Team team = player.getTeam();
        playerRepository.findByTeamAndBackNumber(team, request.getBackNumber())
                .ifPresent(p -> {
                    throw new IllegalArgumentException("이미 사용중인 등번호입니다.");
                });

        player.updateDetails(request.getName(), request.getPosition(), request.getBackNumber());

        return player;
    }

    @Transactional
    public void deletePlayer(Long playerId, UUID ownerId) {

        Player player = playerRepository.findByIdWithTeam(playerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 선수 ID입니다: " + playerId));

        if (!player.getTeam().getOwner().getUserId().equals(ownerId)) {
            throw new IllegalArgumentException("이 선수를 삭제할 권한이 없습니다. (팀 소유자만 가능)");
        }

        playerRepository.delete(player);
    }

    public List<Player> getPlayersByTeamId(Long teamId) {
        return playerRepository.findByTeamTeamIdWithTeam(teamId);
    }
}
