package com.lineupmaker.formation.controller;

import com.lineupmaker.formation.dto.FormationCreateRequest;
import com.lineupmaker.formation.dto.FormationListResponse;
import com.lineupmaker.formation.dto.FormationResponse;
import com.lineupmaker.formation.entity.Formation;
import com.lineupmaker.formation.service.FormationService;
import com.lineupmaker.user.entity.Users;
import com.lineupmaker.user.repository.UserRepository;
import com.lineupmaker.user.service.UserService;
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
@RequestMapping("/api/formation")
@RequiredArgsConstructor
public class FormationController {

    private final FormationService formationService;
    private final UserService userService;
    private final UserRepository userRepository;

    private UUID getUserIdFromAuthentication(Authentication authentication) {
        String userEmail = ((UserDetails) authentication.getPrincipal()).getUsername();
        Users users = userService.findByEmail(userEmail);
        return users.getUserId();
    }

    /**
     * 포메이션 생성 및 선수 배치 API
     * POST /api/formation
     */
    @PostMapping
    public ResponseEntity<Long> createFormation(
            @RequestBody FormationCreateRequest request,
            Authentication authentication) {

        UUID currentUserId = getUserIdFromAuthentication(authentication);
        try {
            Formation formation = formationService.createFormation(request, currentUserId);
            // 생성된 Formation ID를 반환
            return ResponseEntity.status(HttpStatus.CREATED).body(formation.getFormationId());
        } catch (IllegalArgumentException e) {
            // 권한 없음, 팀/선수 ID 오류 시 400 Bad Request
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<FormationListResponse>> getFormations(
            @RequestParam Long teamId,
            Authentication authentication
    ) {
        // 1. JWT 토큰에서 인증된 사용자 이메일 추출
        String userEmail = ((UserDetails) authentication.getPrincipal()).getUsername();

        // 2. 이메일을 사용하여 Users 엔티티를 조회하고 user_id를 추출
        Users user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("인증된 사용자를 찾을 수 없습니다."));

        UUID ownerId = user.getUserId();

        List<Formation> formations = formationService.getFormationsByOwner(ownerId, teamId);

        List<FormationListResponse> responses = formations.stream()
                .map(FormationListResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * 포메이션 상세 조회 API
     * GET /api/formation/{formationId}
     */
    @GetMapping("/{formationId}")
    public ResponseEntity<FormationResponse> getFormation(
            @PathVariable Long formationId,
            Authentication authentication) {

        UUID currentUserId = getUserIdFromAuthentication(authentication);
        try {
            FormationResponse response = formationService.getFormationDetails(formationId, currentUserId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // 포메이션 ID 오류 또는 권한 없음 오류 시 400 Bad Request
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 포메이션 수정 API (이름 및 배치 정보 전체 수정)
     * PUT /api/formation/{formationId}
     */
    @PutMapping("/{formationId}")
    public ResponseEntity<Long> updateFormation(
            @PathVariable Long formationId,
            @RequestBody FormationCreateRequest request, // 생성 DTO를 재사용하여 수정 데이터 받음
            Authentication authentication) {

        UUID currentUserId = getUserIdFromAuthentication(authentication);
        try {
            Formation formation = formationService.updateFormation(formationId, request, currentUserId);
            return ResponseEntity.ok(formation.getFormationId());
        } catch (IllegalArgumentException e) {
            // 권한 없음, ID 오류 시 400 Bad Request
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 포메이션 삭제 API
     * DELETE /api/formation/{formationId}
     */
    @DeleteMapping("/{formationId}")
    public ResponseEntity<Void> deleteFormation(
            @PathVariable Long formationId,
            Authentication authentication) {

        UUID currentUserId = getUserIdFromAuthentication(authentication);
        try {
            formationService.deleteFormation(formationId, currentUserId);
            // 삭제 성공 시 204 No Content 반환
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            // 권한 없음, ID 오류 시 400 Bad Request
            return ResponseEntity.badRequest().build();
        }
    }
}
