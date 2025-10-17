package com.lineupmaker.formation.dto;

import com.lineupmaker.formation.entity.Formation;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class FormationResponse {
    private final Long formationId;
    private final String name;
    private final LocalDateTime createdAt;

    // Team Information
    private final Long teamId;
    private final String teamName;

    // Placement List
    private final List<FormationPlayerResponse> placements;

    public FormationResponse(Formation formation, List<FormationPlayerResponse> placements) {
        this.formationId = formation.getFormationId();
        this.name = formation.getName();
        this.createdAt = formation.getCreatedAt();

        this.teamId = formation.getTeam().getTeamId();
        this.teamName = formation.getTeam().getName();

        this.placements = placements;
    }
}
