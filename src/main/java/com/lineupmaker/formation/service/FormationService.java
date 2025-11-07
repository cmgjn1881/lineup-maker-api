package com.lineupmaker.formation.service;

import com.lineupmaker.formation.dto.FormationCreateRequest;
import com.lineupmaker.formation.dto.FormationPlayerResponse;
import com.lineupmaker.formation.dto.FormationResponse;
import com.lineupmaker.formation.entity.Formation;
import com.lineupmaker.formation.entity.FormationPlayer;
import com.lineupmaker.formation.repository.FormationPlayerRepository;
import com.lineupmaker.formation.repository.FormationRepository;
import com.lineupmaker.player.entity.Player;
import com.lineupmaker.player.repository.PlayerRepository;
import com.lineupmaker.team.entity.Team;
import com.lineupmaker.team.repository.TeamRepository;
import com.lineupmaker.user.entity.Users;
import com.lineupmaker.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FormationService {

    private static final int MAX_FORMATIONS_PER_TEAM = 32;

    private final FormationRepository formationRepository;
    private final FormationPlayerRepository placementRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;

    private final EntityManager entityManager;

    @Transactional
    public Formation createFormation(FormationCreateRequest request, UUID currentUserId) {

        Users user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 팀 ID입니다."));

        if (!team.getOwner().getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("이 팀에 대한 포메이션을 생성할 권한이 없습니다.");
        }

        long currentFormationCount = formationRepository.countByTeamTeamId(request.getTeamId());
        if (currentFormationCount >= MAX_FORMATIONS_PER_TEAM) {
            throw new IllegalStateException("포메이션은 팀당 최대 " + MAX_FORMATIONS_PER_TEAM + "개까지 생성할 수 있습니다.");
        }

        Formation formation = Formation.builder()
                .user(user)
                .team(team)
                .name(request.getName())
                .build();
        formation = formationRepository.save(formation);

        final Formation savedFormation = formation;

        request.getPlacements().forEach(placementDto -> {
            Player player = playerRepository.findById(placementDto.getPlayerId())
                    .orElseThrow(() -> new IllegalArgumentException("선수 ID를 찾을 수 없습니다: " + placementDto.getPlayerId()));

            if (!player.getTeam().getTeamId().equals(request.getTeamId())) {
                throw new IllegalArgumentException("선수가 해당 팀 소속이 아닙니다.");
            }

            FormationPlayer placement = FormationPlayer.builder()
                    .formation(savedFormation)
                    .player(player)
                    .quarter(placementDto.getQuarter())
                    .coordX(placementDto.getCoordX())
                    .coordY(placementDto.getCoordY())
                    .build();

            placementRepository.save(placement);
        });

        return formation;
    }

    @Transactional(readOnly = true)
    public FormationResponse getFormationDetails(Long formationId, UUID currentUserId) {

        // [수정] Fetch Join으로 User와 Team 정보를 함께 가져오는 메소드 사용
        Formation formation = formationRepository.findByIdWithUserAndTeam(formationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 포메이션 ID입니다: " + formationId));

        if (!formation.getTeam().getOwner().getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("이 포메이션을 조회할 권한이 없습니다.");
        }

        List<FormationPlayer> placements = placementRepository.findAllWithPlayerByFormationId(formationId);

        List<FormationPlayerResponse> placementResponses = placements.stream()
                .map(FormationPlayerResponse::new)
                .collect(Collectors.toList());

        return new FormationResponse(formation, placementResponses);
    }

    @Transactional
    public Formation updateFormation(Long formationId, FormationCreateRequest request, UUID currentUserId) {

        // [수정] Fetch Join으로 User와 Team 정보를 함께 가져오는 메소드 사용
        Formation existingFormation = formationRepository.findByIdWithUserAndTeam(formationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 포메이션 ID입니다."));

        if (!existingFormation.getTeam().getOwner().getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("이 포메이션을 수정할 권한이 없습니다.");
        }

        placementRepository.deleteAll(placementRepository.findByFormationFormationId(formationId));

        entityManager.flush();

        existingFormation.updateName(request.getName());

        request.getPlacements().forEach(placementDto -> {
            Player player = playerRepository.findById(placementDto.getPlayerId())
                    .orElseThrow(() -> new IllegalArgumentException("선수 ID를 찾을 수 없습니다: " + placementDto.getPlayerId()));

            if (!player.getTeam().getTeamId().equals(existingFormation.getTeam().getTeamId())) {
                throw new IllegalArgumentException("선수가 해당 팀 소속이 아닙니다.");
            }

            FormationPlayer placement = FormationPlayer.builder()
                    .formation(existingFormation)
                    .player(player)
                    .quarter(placementDto.getQuarter())
                    .coordX(placementDto.getCoordX())
                    .coordY(placementDto.getCoordY())
                    .build();

            placementRepository.save(placement);
        });

        return existingFormation;
    }

    public List<Formation> getFormationsByOwner(UUID userId, Long teamId) {
        return formationRepository.findByUser_UserIdAndTeam_TeamId(userId, teamId);
    }

    @Transactional
    public void deleteFormation(Long formationId, UUID currentUserId) {

        // [수정] Fetch Join으로 User와 Team 정보를 함께 가져오는 메소드 사용
        Formation existingFormation = formationRepository.findByIdWithUserAndTeam(formationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 포메이션 ID입니다."));

        if (!existingFormation.getTeam().getOwner().getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("이 포메이션을 삭제할 권한이 없습니다.");
        }

        List<FormationPlayer> placementsToDelete = placementRepository.findByFormationFormationId(formationId);
        if (!placementsToDelete.isEmpty()) {
            placementRepository.deleteAll(placementsToDelete);

            entityManager.flush();
        }

        formationRepository.delete(existingFormation);
    }

}
