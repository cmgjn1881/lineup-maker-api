package com.lineupmaker.team.service;

import com.lineupmaker.team.dto.TeamCreateRequest;
import com.lineupmaker.team.entity.Team;
import com.lineupmaker.team.repository.TeamRepository;
import com.lineupmaker.user.entity.Users;
import com.lineupmaker.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository; // User 정보 조회를 위해 필요

    @Transactional
    public Team createTeam(TeamCreateRequest request, UUID ownerId) {

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
}
