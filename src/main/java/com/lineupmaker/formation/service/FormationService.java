package com.lineupmaker.formation.service;

import com.lineupmaker.formation.dto.FormationCreateRequest;
import com.lineupmaker.formation.dto.FormationPlayerResponse;
import com.lineupmaker.formation.dto.FormationResponse;
import com.lineupmaker.formation.entity.Formation;
import com.lineupmaker.formation.entity.FormationPlayer;
import com.lineupmaker.formation.entity.FormationQuarterReferee;
import com.lineupmaker.formation.repository.FormationPlayerRepository;
import com.lineupmaker.formation.repository.FormationQuarterRefereeRepository;
import com.lineupmaker.formation.repository.FormationRepository;
import com.lineupmaker.player.entity.Player;
import com.lineupmaker.player.repository.PlayerRepository;
import com.lineupmaker.team.entity.Team;
import com.lineupmaker.team.repository.TeamRepository;
import com.lineupmaker.user.entity.Users;
import com.lineupmaker.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FormationService {

    private static final int MAX_FORMATIONS_PER_TEAM = 32;

    private final FormationRepository formationRepository;
    private final FormationPlayerRepository placementRepository;
    private final FormationQuarterRefereeRepository quarterRefereeRepository; // Repository 추가
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

        // 선수 배치 정보 저장
        if (request.getPlacements() != null) {
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
        }

        // 심판 정보 저장
        saveQuarterReferees(formation, request.getReferees());

        return formation;
    }

    @Transactional(readOnly = true)
    public FormationResponse getFormationDetails(Long formationId, UUID currentUserId) {
        Formation formation = formationRepository.findByIdWithUserAndTeam(formationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 포메이션 ID입니다: " + formationId));

        if (!formation.getTeam().getOwner().getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("이 포메이션을 조회할 권한이 없습니다.");
        }

        List<FormationPlayer> placements = placementRepository.findAllWithPlayerByFormationId(formationId);

        List<FormationPlayerResponse> placementResponses = placements.stream()
                .map(FormationPlayerResponse::new)
                .collect(Collectors.toList());

        // Formation 엔티티에 quarterReferees가 FetchType.LAZY로 로드되므로,
        // DTO 생성자에서 접근하기 전에 명시적으로 초기화해주는 것이 안전합니다.
        // (실제로는 DTO 생성자 내에서 접근 시 프록시가 초기화되지만, 명시하는 것이 좋음)
        formation.getQuarterReferees().size(); // 프록시 초기화

        return new FormationResponse(formation, placementResponses);
    }

    @Transactional
    public Formation updateFormation(Long formationId, FormationCreateRequest request, UUID currentUserId) {
        Formation existingFormation = formationRepository.findByIdWithUserAndTeam(formationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 포메이션 ID입니다."));

        if (!existingFormation.getTeam().getOwner().getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("이 포메이션을 수정할 권한이 없습니다.");
        }

        // 기존 정보 삭제 (선수 배치 및 심판 정보)
        placementRepository.deleteAll(placementRepository.findByFormationFormationId(formationId));
        quarterRefereeRepository.deleteByFormation(existingFormation); // 기존 심판 정보 삭제

        entityManager.flush();

        existingFormation.updateName(request.getName());

        // 새로운 정보 저장 (선수 배치)
        if (request.getPlacements() != null) {
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
        }

        // 새로운 정보 저장 (심판)
        saveQuarterReferees(existingFormation, request.getReferees());

        return existingFormation;
    }

    public List<Formation> getFormationsByOwner(UUID userId, Long teamId, String sort) {
        Sort sorting = Sort.unsorted();
        if (StringUtils.hasText(sort)) {
            if ("createdAt".equalsIgnoreCase(sort)) {
                sorting = Sort.by(Sort.Direction.DESC, "createdAt");
            } else if ("name".equalsIgnoreCase(sort)) {
                sorting = Sort.by(Sort.Direction.ASC, "name");
            }
        }
        return formationRepository.findByUser_UserIdAndTeam_TeamId(userId, teamId, sorting);
    }

    @Transactional
    public void deleteFormation(Long formationId, UUID currentUserId) {
        Formation existingFormation = formationRepository.findByIdWithUserAndTeam(formationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 포메이션 ID입니다."));

        if (!existingFormation.getTeam().getOwner().getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("이 포메이션을 삭제할 권한이 없습니다.");
        }
        
        // Formation 엔티티의 Cascade 설정으로 인해 FormationPlayer와 FormationQuarterReferee는 자동으로 삭제됨
        formationRepository.delete(existingFormation);
    }

    // 심판 정보 저장을 위한 헬퍼 메소드
    private void saveQuarterReferees(Formation formation, Map<Integer, String> referees) {
        if (referees != null && !referees.isEmpty()) {
            referees.forEach((quarter, refereeName) -> {
                if (StringUtils.hasText(refereeName)) { // 심판 이름이 있는 경우에만 저장
                    FormationQuarterReferee fqr = FormationQuarterReferee.builder()
                            .formation(formation)
                            .quarter(quarter)
                            .refereeName(refereeName)
                            .build();
                    quarterRefereeRepository.save(fqr);
                }
            });
        }
    }
}
