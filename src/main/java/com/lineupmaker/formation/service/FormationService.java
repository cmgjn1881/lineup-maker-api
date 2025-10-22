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

    private final FormationRepository formationRepository;
    private final FormationPlayerRepository placementRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;

    private final EntityManager entityManager;

    /**
     * 포메이션 생성 및 선수 배치 (일괄 저장)
     * @param request 포메이션 정보와 선수 배치 목록
     * @param currentUserId 요청한 사용자 ID (소유자)
     * @return 생성된 포메이션 엔티티
     */
    @Transactional
    public Formation createFormation(FormationCreateRequest request, UUID currentUserId) {

        // 1. 사용자 및 팀 조회 (권한 검증)
        Users user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 팀 ID입니다."));

        // [핵심 보안 검증] 팀 소유자만 포메이션 생성 가능
        if (!team.getOwner().getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("이 팀에 대한 포메이션을 생성할 권한이 없습니다.");
        }

        // 2. Formation 엔티티 생성 및 저장
        Formation formation = Formation.builder()
                .user(user)
                .team(team)
                .name(request.getName())
                .build();
        formation = formationRepository.save(formation);

        // 3. 선수 배치 정보 처리 및 저장
        final Formation savedFormation = formation;

        request.getPlacements().forEach(placementDto -> {
            // 선수 ID로 선수 엔티티 조회
            Player player = playerRepository.findById(placementDto.getPlayerId())
                    .orElseThrow(() -> new IllegalArgumentException("선수 ID를 찾을 수 없습니다: " + placementDto.getPlayerId()));

            // 선수가 해당 팀 소속인지 확인 (데이터 무결성)
            if (!player.getTeam().getTeamId().equals(request.getTeamId())) {
                throw new IllegalArgumentException("선수가 해당 팀 소속이 아닙니다.");
            }

            // FormationPlayer 엔티티 생성
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

    /**
     * 포메이션 상세 조회
     * @param formationId 조회할 포메이션 ID
     * @param currentUserId 요청 사용자 ID
     * @return 포메이션 상세 정보 DTO
     */
    public FormationResponse getFormationDetails(Long formationId, UUID currentUserId) {

        // 1. 포메이션 조회 (Team과 User 정보가 함께 로딩되도록 Fetch Join 고려 가능하지만, 지금은 Eager/Lazy 기본 동작에 의존)
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 포메이션 ID입니다: " + formationId));

        // 2. [핵심 보안 검증] 팀 소유자만 조회 가능 (생성 권한과 동일)
        if (!formation.getTeam().getOwner().getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("이 포메이션을 조회할 권한이 없습니다.");
        }

        // 3. 해당 포메이션에 연결된 모든 선수 배치 정보 조회
        List<FormationPlayer> placements = placementRepository.findByFormationFormationId(formationId);

        // 4. DTO 변환
        List<FormationPlayerResponse> placementResponses = placements.stream()
                .map(FormationPlayerResponse::new)
                .collect(Collectors.toList());

        return new FormationResponse(formation, placementResponses);
    }

    /**
     * 포메이션 수정 (기존 배치 정보 삭제 후 새로운 정보로 대체)
     * @param formationId 수정할 포메이션 ID
     * @param request 새 포메이션 정보와 선수 배치 목록
     * @param currentUserId 요청 사용자 ID (소유자)
     * @return 수정된 포메이션 엔티티
     */
    @Transactional
    public Formation updateFormation(Long formationId, FormationCreateRequest request, UUID currentUserId) {

        // 1. 기존 포메이션 조회 및 권한 검증
        Formation existingFormation = formationRepository.findById(formationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 포메이션 ID입니다."));

        // [핵심 보안 검증] 팀 소유자만 수정 가능
        if (!existingFormation.getTeam().getOwner().getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("이 포메이션을 수정할 권한이 없습니다.");
        }

        // 2. 기존 배치 정보 전체 삭제 (덮어쓰기)
        placementRepository.deleteAll(placementRepository.findByFormationFormationId(formationId));

        // 삭제 쿼리(DELETE)를 DB에 즉시 반영하여 제약 조건 위반을 방지
        entityManager.flush();

        // 3. 포메이션 이름 업데이트 (Dirty Checking 활용)
        existingFormation.updateName(request.getName());

        // 4. 새로운 선수 배치 정보 처리 및 저장 (createFormation 로직 재사용)
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

        // Dirty Checking을 위해 엔티티 반환
        return existingFormation;
    }

    public List<Formation> getFormationsByOwner(UUID userId, Long teamId) {
        return formationRepository.findByUser_UserIdAndTeam_TeamId(userId, teamId);
    }

    /**
     * 포메이션 삭제
     * @param formationId 삭제할 포메이션 ID
     * @param currentUserId 요청 사용자 ID (소유자)
     */
    @Transactional
    public void deleteFormation(Long formationId, UUID currentUserId) {

        // 1. 기존 포메이션 조회 및 권한 검증
        Formation existingFormation = formationRepository.findById(formationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 포메이션 ID입니다."));

        // [핵심 보안 검증] 팀 소유자만 삭제 가능
        if (!existingFormation.getTeam().getOwner().getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("이 포메이션을 삭제할 권한이 없습니다.");
        }

        // 2. 포메이션 삭제 (연관된 FormationPlayer는 ON DELETE CASCADE로 자동 삭제됨)
        formationRepository.delete(existingFormation);
    }

}
