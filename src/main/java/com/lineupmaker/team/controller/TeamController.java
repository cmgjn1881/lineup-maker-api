package com.lineupmaker.team.controller;

import com.lineupmaker.team.dto.TeamCreateRequest;
import com.lineupmaker.team.dto.TeamResponse;
import com.lineupmaker.team.dto.TeamUpdateRequest;
import com.lineupmaker.team.entity.Team;
import com.lineupmaker.team.service.TeamService;
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
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final UserRepository userRepository; // user_id 조회를 위해 필요

    // 새로운 팀 생성 (Access Token 필요)
    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(@RequestBody TeamCreateRequest request, Authentication authentication) {

        // 1. JWT 토큰으로 인증된 사용자 이메일 추출
        String userEmail = ((UserDetails) authentication.getPrincipal()).getUsername();

        // 2. 이메일을 사용하여 Users 엔티티를 조회하고 user_id를 추출
        Users user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("인증된 사용자를 찾을 수 없습니다."));

        UUID ownerId = user.getUserId();

        // 3. 서비스 호출 및 팀 생성
        Team newTeam = teamService.createTeam(request, ownerId);

        // 4. 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(new TeamResponse(newTeam));
    }

    // 인증된 사용자의 팀 목록을 조회
    @GetMapping
    public ResponseEntity<List<TeamResponse>> getMyTeams(Authentication authentication) {

        // 1. JWT 토큰에서 인증된 사용자 이메일 추출
        String userEmail = ((UserDetails) authentication.getPrincipal()).getUsername();

        // 2. 이메일을 사용하여 Users 엔티티를 조회하고 user_id를 추출
        Users user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("인증된 사용자를 찾을 수 없습니다."));

        UUID ownerId = user.getUserId();

        // 3. 서비스 호출 및 팀 목록 조회
        List<Team> teams = teamService.getTeamsByOwner(ownerId);

        // 4. TeamResponse DTO 목록으로 변환하여 반환
        List<TeamResponse> responses = teams.stream()
                .map(TeamResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    // 특정 팀 이름을 수정합니다.
    @PutMapping("/{teamId}")
    public ResponseEntity<TeamResponse> updateTeam(
            @PathVariable Long teamId,
            @RequestBody TeamUpdateRequest request,
            Authentication authentication) {

        // 1. JWT 토큰에서 인증된 사용자 이메일 추출
        String userEmail = ((UserDetails) authentication.getPrincipal()).getUsername();

        // 2. 이메일을 사용하여 Users 엔티티를 조회하고 user_id를 추출
        Users user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("인증된 사용자를 찾을 수 없습니다."));
        UUID ownerId = user.getUserId();

        Team updatedTeam = teamService.updateTeam(teamId, request, ownerId);

        return ResponseEntity.ok(new TeamResponse(updatedTeam));
    }

    // 특정 팀을 삭제 합니다.
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long teamId, Authentication authentication) {

        // 1. JWT 토큰에서 인증된 사용자 이메일 추출
        String userEmail = ((UserDetails) authentication.getPrincipal()).getUsername();

        // 2. 이메일을 사용하여 Users 엔티티를 조회하고 user_id를 추출
        Users user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("인증된 사용자를 찾을 수 없습니다."));
        UUID ownerId = user.getUserId();

        // 3. 서비스 호출 및 삭제
        teamService.deleteTeam(teamId, ownerId);

        // 4. 응답 반환 (204 No Content: 성공적으로 삭제되었지만 본문은 없음)
        return ResponseEntity.noContent().build();
    }
}
