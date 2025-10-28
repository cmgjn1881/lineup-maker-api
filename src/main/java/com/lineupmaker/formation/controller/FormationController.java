package com.lineupmaker.formation.controller;

import com.lineupmaker.formation.dto.FormationCreateRequest;
import com.lineupmaker.formation.dto.FormationListResponse;
import com.lineupmaker.formation.dto.FormationResponse;
import com.lineupmaker.formation.entity.Formation;
import com.lineupmaker.formation.service.FormationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

@Tag(name = "포메이션")
@RestController
@RequestMapping("/api/formation")
@RequiredArgsConstructor
public class FormationController {

    private final FormationService formationService;

    @Operation(summary = "포메이션 생성", description = "새로운 포메이션을 생성합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<Long> createFormation(
            @RequestBody FormationCreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        UUID currentUserId = UUID.fromString(userDetails.getUsername());
        try {
            Formation formation = formationService.createFormation(request, currentUserId);
            return ResponseEntity.status(HttpStatus.CREATED).body(formation.getFormationId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "팀의 포메이션 목록 조회", description = "특정 팀에 속한 모든 포메이션 목록을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<List<FormationListResponse>> getFormations(
            @Parameter(description = "포메이션을 조회할 팀의 ID") @RequestParam Long teamId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        UUID ownerId = UUID.fromString(userDetails.getUsername());
        List<Formation> formations = formationService.getFormationsByOwner(ownerId, teamId);
        List<FormationListResponse> responses = formations.stream()
                .map(FormationListResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "단일 포메이션 상세 조회", description = "특정 포메이션의 상세 정보를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{formationId}")
    public ResponseEntity<FormationResponse> getFormation(
            @Parameter(description = "조회할 포메이션의 ID") @PathVariable Long formationId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        UUID currentUserId = UUID.fromString(userDetails.getUsername());
        try {
            FormationResponse response = formationService.getFormationDetails(formationId, currentUserId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "포메이션 수정", description = "기존 포메이션의 정보를 수정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{formationId}")
    public ResponseEntity<Long> updateFormation(
            @Parameter(description = "수정할 포메이션의 ID") @PathVariable Long formationId,
            @RequestBody FormationCreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        UUID currentUserId = UUID.fromString(userDetails.getUsername());
        try {
            Formation formation = formationService.updateFormation(formationId, request, currentUserId);
            return ResponseEntity.ok(formation.getFormationId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "포메이션 삭제", description = "특정 포메이션을 삭제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{formationId}")
    public ResponseEntity<Void> deleteFormation(
            @Parameter(description = "삭제할 포메이션의 ID") @PathVariable Long formationId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        UUID currentUserId = UUID.fromString(userDetails.getUsername());
        try {
            formationService.deleteFormation(formationId, currentUserId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
