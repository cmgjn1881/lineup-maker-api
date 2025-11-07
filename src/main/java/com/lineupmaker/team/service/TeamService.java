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
    private final UserRepository userRepository;

    @Transactional
    public Team createTeam(TeamCreateRequest request, UUID ownerId) {

        long currentTeamCount = teamRepository.countByOwnerUserId(ownerId);
        if (currentTeamCount >= MAX_TEAMS_PER_USER) {
            throw new IllegalStateException("팀은 최대 " + MAX_TEAMS_PER_USER + "개까지 생성할 수 있습니다.");
        }

        Users owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new UsernameNotFoundException("팀 소유자를 찾을 수 없습니다."));

        Team newTeam = Team.builder()
                .owner(owner)
                .name(request.getName())
                .build();

        return teamRepository.save(newTeam);
    }

    public List<Team> getTeamsByOwner(UUID ownerId) {
        return teamRepository.findByOwnerUserIdWithUser(ownerId);
    }

    @Transactional
    public Team updateTeam(Long teamId, TeamUpdateRequest request, UUID ownerId) {

        Team teamToUpdate = teamRepository.findByIdWithUser(teamId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 팀 ID입니다: " + teamId));

        if (!teamToUpdate.getOwner().getUserId().equals(ownerId)) {
            throw new AccessDeniedException("팀을 수정할 권한이 없습니다. (소유자만 가능)");
        }

        teamToUpdate.updateName(request.newName());

        return teamRepository.save(teamToUpdate);
    }

    @Transactional
    public void deleteTeam(Long teamId, UUID ownerId) {

        Team teamToDelete = teamRepository.findByIdWithUser(teamId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 팀 ID입니다: " + teamId));

        if (!teamToDelete.getOwner().getUserId().equals(ownerId)) {
            throw new AccessDeniedException("팀을 수정할 권한이 없습니다. (소유자만 가능)");
        }

        teamRepository.deleteById(teamId);
    }
}
