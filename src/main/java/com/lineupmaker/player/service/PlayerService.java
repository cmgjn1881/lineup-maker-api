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

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;

    /**
     * 선수 정보 추가
     */
    @Transactional
    public Player createPlayer(Long teamId, PlayerCreateRequest request, UUID ownerId) {

        // 1. 팀 조회 (없으면 404)
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 팀 ID입니다: " + teamId));

        // 2. [핵심 보안 검증] 요청한 사용자가 팀의 소유자인지 확인
        if (!team.getOwner().getUserId().equals(ownerId)) {
            throw new IllegalArgumentException("이 팀에 선수를 추가할 권한이 없습니다. (팀 소유자만 가능)");
        }

        // 3. Player 엔티티 생성
        Player newPlayer = Player.builder()
                .team(team)
                .name(request.getName())
                .position(request.getPosition())
                .backNumber(request.getBackNumber())
                .build();

        // 4. DB에 저장
        return playerRepository.save(newPlayer);
    }

    /**
     * 선수 정보 수정
     */
    @Transactional
    public Player updatePlayer(Long playerId, PlayerUpdateRequest request, UUID ownerId) {

        // 1. 선수 조회
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 선수 ID입니다: " + playerId));

        // 2. 팀 소유자만 수정 가능
        if (!player.getTeam().getOwner().getUserId().equals(ownerId)) {
            throw new IllegalArgumentException("이 선수를 수정할 권한이 없습니다. (팀 소유자만 가능)");
        }

        // 3. Dirty Checking을 통해 정보 업데이트
        player.updateDetails(request.getName(), request.getPosition(), request.getBackNumber());

        // 4. save() 없이 트랜잭션 종료 시 자동 반영 (Dirty Checking)
        return player;
    }

    /**
     * 선수 삭제
     */
    @Transactional
    public void deletePlayer(Long playerId, UUID ownerId) {

        // 1. 선수 조회
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 선수 ID입니다: " + playerId));

        // 2. 팀 소유자만 삭제 가능
        if (!player.getTeam().getOwner().getUserId().equals(ownerId)) {
            throw new IllegalArgumentException("이 선수를 삭제할 권한이 없습니다. (팀 소유자만 가능)");
        }

        // 3. DB에서 삭제
        playerRepository.delete(player);
    }


    /**
     * 특정 팀의 선수 목록을 조회합니다.
     * @param teamId 선수 목록을 조회할 팀 ID
     * @return Player 엔티티 목록
     */
    public List<Player> getPlayersByTeamId(Long teamId) {
        return playerRepository.findByTeamTeamId(teamId);
    }
}
