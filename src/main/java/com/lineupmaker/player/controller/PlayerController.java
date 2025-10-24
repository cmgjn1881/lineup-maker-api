package com.lineupmaker.player.controller;

import com.lineupmaker.player.dto.PlayerCreateRequest;
import com.lineupmaker.player.dto.PlayerResponse;
import com.lineupmaker.player.dto.PlayerUpdateRequest;
import com.lineupmaker.player.entity.Player;
import com.lineupmaker.player.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping
    public ResponseEntity<PlayerResponse> createPlayer(
            @PathVariable Long teamId,
            @RequestBody PlayerCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID currentOwnerId = UUID.fromString(userDetails.getUsername());
        Player newPlayer = playerService.createPlayer(teamId, request, currentOwnerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new PlayerResponse(newPlayer));
    }

    @GetMapping
    public ResponseEntity<List<PlayerResponse>> getPlayers(@PathVariable Long teamId) {
        List<Player> players = playerService.getPlayersByTeamId(teamId);
        List<PlayerResponse> response = players.stream()
                .map(PlayerResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{playerId}", produces = "application/json")
    public ResponseEntity<PlayerResponse> updatePlayer(
            @PathVariable Long playerId,
            @RequestBody PlayerUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID ownerId = UUID.fromString(userDetails.getUsername());
        Player updatedPlayer = playerService.updatePlayer(playerId, request, ownerId);
        return ResponseEntity.ok(new PlayerResponse(updatedPlayer));
    }

    @DeleteMapping("/{playerId}")
    public ResponseEntity<Void> deletePlayer(
            @PathVariable Long playerId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID ownerId = UUID.fromString(userDetails.getUsername());
        playerService.deletePlayer(playerId, ownerId);
        return ResponseEntity.noContent().build();
    }
}
