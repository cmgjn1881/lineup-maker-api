package com.lineupmaker.formation.controller;

import com.lineupmaker.formation.dto.FormationCreateRequest;
import com.lineupmaker.formation.dto.FormationResponse;
import com.lineupmaker.formation.entity.Formation;
import com.lineupmaker.formation.service.FormationService;
import com.lineupmaker.user.entity.Users;
import com.lineupmaker.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/formation")
@RequiredArgsConstructor
public class FormationController {

    private final FormationService formationService;
    private final UserService userService;

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
