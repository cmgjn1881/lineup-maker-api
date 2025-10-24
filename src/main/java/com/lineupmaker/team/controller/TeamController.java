package com.lineupmaker.team.controller;

import com.lineupmaker.team.dto.TeamCreateRequest;
import com.lineupmaker.team.dto.TeamResponse;
import com.lineupmaker.team.dto.TeamUpdateRequest;
import com.lineupmaker.team.entity.Team;
import com.lineupmaker.team.service.TeamService;
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
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(
            @RequestBody TeamCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID ownerId = UUID.fromString(userDetails.getUsername());
        Team newTeam = teamService.createTeam(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new TeamResponse(newTeam));
    }

    @GetMapping
    public ResponseEntity<List<TeamResponse>> getMyTeams(@AuthenticationPrincipal UserDetails userDetails) {
        UUID ownerId = UUID.fromString(userDetails.getUsername());
        List<Team> teams = teamService.getTeamsByOwner(ownerId);
        List<TeamResponse> responses = teams.stream()
                .map(TeamResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{teamId}")
    public ResponseEntity<TeamResponse> updateTeam(
            @PathVariable Long teamId,
            @RequestBody TeamUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID ownerId = UUID.fromString(userDetails.getUsername());
        Team updatedTeam = teamService.updateTeam(teamId, request, ownerId);
        return ResponseEntity.ok(new TeamResponse(updatedTeam));
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID ownerId = UUID.fromString(userDetails.getUsername());
        teamService.deleteTeam(teamId, ownerId);
        return ResponseEntity.noContent().build();
    }
}
