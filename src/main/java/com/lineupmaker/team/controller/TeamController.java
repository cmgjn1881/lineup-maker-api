package com.lineupmaker.team.controller;

import com.lineupmaker.team.dto.TeamCreateRequest;
import com.lineupmaker.team.dto.TeamResponse;
import com.lineupmaker.team.dto.TeamUpdateRequest;
import com.lineupmaker.team.entity.Team;
import com.lineupmaker.team.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@Tag(name = "팀")
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @Operation(summary = "팀 생성", description = "새로운 팀을 생성합니다.")
    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(
            @RequestBody TeamCreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        UUID ownerId = UUID.fromString(userDetails.getUsername());
        Team newTeam = teamService.createTeam(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new TeamResponse(newTeam));
    }

    @Operation(summary = "내 팀 목록 조회", description = "현재 로그인한 사용자가 소유한 모든 팀 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<TeamResponse>> getMyTeams(@Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        UUID ownerId = UUID.fromString(userDetails.getUsername());
        List<Team> teams = teamService.getTeamsByOwner(ownerId);
        List<TeamResponse> responses = teams.stream()
                .map(TeamResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "팀 정보 수정", description = "특정 팀의 정보를 수정합니다.")
    @PutMapping("/{teamId}")
    public ResponseEntity<TeamResponse> updateTeam(
            @Parameter(description = "수정할 팀의 ID") @PathVariable Long teamId,
            @RequestBody TeamUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        UUID ownerId = UUID.fromString(userDetails.getUsername());
        Team updatedTeam = teamService.updateTeam(teamId, request, ownerId);
        return ResponseEntity.ok(new TeamResponse(updatedTeam));
    }

    @Operation(summary = "팀 삭제", description = "특정 팀을 삭제합니다.")
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteTeam(
            @Parameter(description = "삭제할 팀의 ID") @PathVariable Long teamId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        UUID ownerId = UUID.fromString(userDetails.getUsername());
        teamService.deleteTeam(teamId, ownerId);
        return ResponseEntity.noContent().build();
    }
}
