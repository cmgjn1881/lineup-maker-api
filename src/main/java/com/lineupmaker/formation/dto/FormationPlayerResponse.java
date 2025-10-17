package com.lineupmaker.formation.dto;

import com.lineupmaker.formation.entity.FormationPlayer;
import lombok.Getter;

@Getter
public class FormationPlayerResponse {
    // Player Information
    private final Long playerId;
    private final String playerName;
    private final String playerPosition;
    private final Integer playerBackNumber;

    // Placement Information
    private final Integer quarter;
    private final Integer coordX;
    private final Integer coordY;

    public FormationPlayerResponse(FormationPlayer placement) {
        this.playerId = placement.getPlayer().getPlayerId();
        this.playerName = placement.getPlayer().getName();
        this.playerPosition = placement.getPlayer().getPosition();
        this.playerBackNumber = placement.getPlayer().getBackNumber();
        this.quarter = placement.getQuarter();
        this.coordX = placement.getCoordX();
        this.coordY = placement.getCoordY();
    }
}

