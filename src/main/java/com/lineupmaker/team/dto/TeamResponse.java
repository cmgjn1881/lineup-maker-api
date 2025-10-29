package com.lineupmaker.team.dto;

import com.lineupmaker.team.entity.Team;
import lombok.Getter;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

@Getter
public class TeamResponse {
    private final Long teamId;
    private final String name;
    private final UUID ownerId;
    private final String ownerEmail;
    private final Instant createdAt;

    public TeamResponse(Team team) {
        this.teamId = team.getTeamId();
        this.name = team.getName();
        this.ownerId = team.getOwner().getUserId();
        this.ownerEmail = team.getOwner().getEmail();
        this.createdAt = team.getCreatedAt().toInstant(ZoneOffset.UTC);
    }
}
