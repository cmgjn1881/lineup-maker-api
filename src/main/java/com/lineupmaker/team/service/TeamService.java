package com.lineupmaker.team.service;

import com.lineupmaker.team.dto.TeamCreateRequest;
import com.lineupmaker.team.dto.TeamUpdateRequest;
import com.lineupmaker.team.entity.Team;
import com.lineupmaker.team.repository.TeamRepository;
import com.lineupmaker.user.entity.Users;
import com.lineupmaker.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private static final int MAX_TEAMS_PER_USER = 3;

    private final TeamRepository teamRepository;
    private final UserRepository userRepository; // User 정보 조회를 위해 필요

    @Transactional
    public Team createTeam(TeamCreateRequest request, UUID ownerId) {

        // [추가] 팀 생성 개수 제한 확인
        long currentTeamCount = teamRepository.countByOwnerUserId(ownerId);
        if (currentTeamCount >= MAX_TEAMS_PER_USER) {
            throw new IllegalStateException("팀은 최대 " + MAX_TEAMS_PER_USER + "개까지 생성할 수 있습니다.");
        }

        // 1. ownerId를 사용하여 Users 엔티티를 조회 (유효성 및 매핑을 위해 필수)
        Users owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new UsernameNotFoundException("팀 소유자를 찾을 수 없습니다."));

        // 2. 팀 엔티티 생성
        Team newTeam = Team.builder()
                .owner(owner)
                .name(request.getName())
                .build();

        // 3. DB에 저장
        return teamRepository.save(newTeam);
    }

    public List<Team> getTeamsByOwner(UUID ownerId) {
        return teamRepository.findByOwnerUserId(ownerId);
    }

    // 특정 팀 수정
    @Transactional
    public Team updateTeam(Long teamId, TeamUpdateRequest request, UUID ownerId) {

        // 1. 팀 조회 (없으면 404)
        Team teamToUpdate = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 팀 ID입니다: " + teamId));

        // 2. [핵심 보안 검증] 요청한 사용자가 팀의 소유자인지 확인
        if (!teamToUpdate.getOwner().getUserId().equals(ownerId)) {
            throw new AccessDeniedException("팀을 수정할 권한이 없습니다. (소유자만 가능)");
        }

        // 3. 팀 이름 변경 (Team 엔티티에 setter 또는 변경 메서드 필요)
        teamToUpdate.updateName(request.newName()); // 엔티티 메서드 호출

        // 4. JPA의 Dirty Checking으로 자동 저장되지만, 명시적으로 save 가능
        return teamRepository.save(teamToUpdate);
    }

    // 특정 팀 삭제
    @Transactional
    public void deleteTeam(Long teamId, UUID ownerId) {

        // 1. 팀 조회 (없으면 404)
        Team teamToUpdate = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 팀 ID입니다: " + teamId));

        // 2. [핵심 보안 검증] 요청한 사용자가 팀의 소유자인지 확인
        if (!teamToUpdate.getOwner().getUserId().equals(ownerId)) {
            throw new AccessDeniedException("팀을 수정할 권한이 없습니다. (소유자만 가능)");
        }

        // 3. 팀 삭제
        teamRepository.deleteById(teamId);
    }
}
