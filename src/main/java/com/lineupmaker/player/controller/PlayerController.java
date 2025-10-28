package com.lineupmaker.player.controller;

import com.lineupmaker.player.dto.PlayerCreateRequest;
import com.lineupmaker.player.dto.PlayerResponse;
import com.lineupmaker.player.dto.PlayerUpdateRequest;
import com.lineupmaker.player.entity.Player;
import com.lineupmaker.player.service.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "선수")
@RestController
@RequestMapping("/api/teams/{teamId}/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @Operation(summary = "팀에 선수 추가", description = "특정 팀에 새로운 선수를 추가합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<PlayerResponse> createPlayer(
            @Parameter(description = "선수를 추가할 팀의 ID") @PathVariable Long teamId,
            @RequestBody PlayerCreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        UUID currentOwnerId = UUID.fromString(userDetails.getUsername());
        Player newPlayer = playerService.createPlayer(teamId, request, currentOwnerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new PlayerResponse(newPlayer));
    }

    @Operation(summary = "팀의 선수 목록 조회", description = "특정 팀에 속한 모든 선수 목록을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<List<PlayerResponse>> getPlayers(
            @Parameter(description = "선수 목록을 조회할 팀의 ID") @PathVariable Long teamId) {
        List<Player> players = playerService.getPlayersByTeamId(teamId);
        List<PlayerResponse> response = players.stream()
                .map(PlayerResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "선수 정보 수정", description = "특정 선수의 정보를 수정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping(value = "/{playerId}", produces = "application/json")
    public ResponseEntity<PlayerResponse> updatePlayer(
            @Parameter(description = "수정할 선수의 ID") @PathVariable Long playerId,
            @RequestBody PlayerUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        UUID ownerId = UUID.fromString(userDetails.getUsername());
        Player updatedPlayer = playerService.updatePlayer(playerId, request, ownerId);
        return ResponseEntity.ok(new PlayerResponse(updatedPlayer));
    }

    @Operation(summary = "선수 삭제", description = "특정 선수를 팀에서 삭제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{playerId}")
    public ResponseEntity<Void> deletePlayer(
            @Parameter(description = "삭제할 선수의 ID") @PathVariable Long playerId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        UUID ownerId = UUID.fromString(userDetails.getUsername());
        playerService.deletePlayer(playerId, ownerId);
        return ResponseEntity.noContent().build();
    }
}
