package com.lineupmaker.formation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.lineupmaker.formation.entity.Formation;
import lombok.Getter;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class FormationResponse {
    private final Long formationId;
    private final String name;
    private final Instant createdAt;

    // Team Information
    private final Long teamId;
    private final String teamName;

    // Placement List
    @Schema(description = "쿼터별 심판 정보. Key는 쿼터(1~4), Value는 심판 이름입니다.",
            example = "{\"1\": \"홍길동\", \"2\": \"김철수\", \"3\": \"홍길동\", \"4\": \"이영희\"}")
    private final List<FormationPlayerResponse> placements;

    // Referee Information
    private final Map<Integer, String> referees;

    public FormationResponse(Formation formation, List<FormationPlayerResponse> placements) {
        this.formationId = formation.getFormationId();
        this.name = formation.getName();
        this.createdAt = formation.getCreatedAt().toInstant(ZoneOffset.UTC);

        this.teamId = formation.getTeam().getTeamId();
        this.teamName = formation.getTeam().getName();

        this.placements = placements;

        this.referees = formation.getQuarterReferees().stream()
                .collect(Collectors.toMap(
                        qr -> qr.getQuarter(),
                        qr -> qr.getRefereeName()
                ));
    }
}
