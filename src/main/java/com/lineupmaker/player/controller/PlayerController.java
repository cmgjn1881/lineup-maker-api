package com.lineupmaker.player.controller;

import com.lineupmaker.player.dto.PlayerCreateRequest;
import com.lineupmaker.player.dto.PlayerResponse;
import com.lineupmaker.player.dto.PlayerUpdateRequest;
import com.lineupmaker.player.entity.Player;
import com.lineupmaker.player.service.PlayerService;
import com.lineupmaker.user.entity.Users;
import com.lineupmaker.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teams/{teamId}/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;
    private final UserRepository userRepository;

    // 현재 인증된 사용자 ID를 추출하는 헬퍼 메서드
    private UUID getCurrentUserId(Authentication authentication) {
        String userEmail = ((UserDetails) authentication.getPrincipal()).getUsername();
        Users user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("인증된 사용자를 찾을 수 없습니다."));
        return user.getUserId();
    }

    /**
     * 특정 팀에 새로운 선수를 추가합니다. (팀 소유자만 가능)
     * POST /api/teams/{teamId}/players
     */
    @PostMapping
    public ResponseEntity<PlayerResponse> createPlayer(
            @PathVariable Long teamId,
            @RequestBody PlayerCreateRequest request,
            Authentication authentication) {

        UUID currentOwnerId = getCurrentUserId(authentication);

        // 서비스 호출 및 선수 생성
        Player newPlayer = playerService.createPlayer(teamId, request, currentOwnerId);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(new PlayerResponse(newPlayer));
    }

    /**
     * 특정 팀의 선수 목록을 조회합니다.
     * GET /api/teams/{teamId}/players
     */
    @GetMapping
    public ResponseEntity<List<PlayerResponse>> getPlayers(@PathVariable Long teamId) {

        // 팀 ID만 사용하여 선수 목록 조회 (이 API는 인증 없이도 조회 가능하게 설계)
        List<Player> players = playerService.getPlayersByTeamId(teamId);

        // DTO 목록으로 변환하여 반환
        List<PlayerResponse> response = players.stream()
                .map(PlayerResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 3. 선수 정보 수정 API (PUT)
     */
    @PutMapping(value = "/{playerId}", produces = "application/json")
    public ResponseEntity<PlayerResponse> updatePlayer(
            @PathVariable Long playerId,
            @RequestBody PlayerUpdateRequest request,
            Authentication authentication) {

        UUID ownerId = getCurrentUserId(authentication);

        // 🔑 [수정] try-catch 블록을 제거합니다.
        // Service에서 발생한 IllegalArgumentException은 GlobalExceptionHandler로 전달됩니다.
        Player updatedPlayer = playerService.updatePlayer(playerId, request, ownerId);

        return ResponseEntity.ok(new PlayerResponse(updatedPlayer));
    }

    /**
     * 4. 선수 삭제 API (DELETE)
     */
    @DeleteMapping("/{playerId}")
    public ResponseEntity<Void> deletePlayer(
            @PathVariable Long playerId,
            Authentication authentication) {

        UUID ownerId = getCurrentUserId(authentication);
        try {
            playerService.deletePlayer(playerId, ownerId);
            // 204 No Content 반환 (RESTful 표준: 성공적인 삭제)
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            // 권한 없음 또는 선수 ID 오류 발생 시 400 Bad Request
            return ResponseEntity.badRequest().build();
        }
    }
}
