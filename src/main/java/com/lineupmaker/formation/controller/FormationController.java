package com.lineupmaker.formation.controller;

import com.lineupmaker.formation.dto.FormationCreateRequest;
import com.lineupmaker.formation.dto.FormationListResponse;
import com.lineupmaker.formation.dto.FormationResponse;
import com.lineupmaker.formation.entity.Formation;
import com.lineupmaker.formation.service.FormationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping
    public ResponseEntity<Long> createFormation(
            @RequestBody FormationCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID currentUserId = UUID.fromString(userDetails.getUsername());
        try {
            Formation formation = formationService.createFormation(request, currentUserId);
            return ResponseEntity.status(HttpStatus.CREATED).body(formation.getFormationId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<FormationListResponse>> getFormations(
            @RequestParam Long teamId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID ownerId = UUID.fromString(userDetails.getUsername());
        List<Formation> formations = formationService.getFormationsByOwner(ownerId, teamId);
        List<FormationListResponse> responses = formations.stream()
                .map(FormationListResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{formationId}")
    public ResponseEntity<FormationResponse> getFormation(
            @PathVariable Long formationId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID currentUserId = UUID.fromString(userDetails.getUsername());
        try {
            FormationResponse response = formationService.getFormationDetails(formationId, currentUserId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{formationId}")
    public ResponseEntity<Long> updateFormation(
            @PathVariable Long formationId,
            @RequestBody FormationCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID currentUserId = UUID.fromString(userDetails.getUsername());
        try {
            Formation formation = formationService.updateFormation(formationId, request, currentUserId);
            return ResponseEntity.ok(formation.getFormationId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{formationId}")
    public ResponseEntity<Void> deleteFormation(
            @PathVariable Long formationId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID currentUserId = UUID.fromString(userDetails.getUsername());
        try {
            formationService.deleteFormation(formationId, currentUserId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
