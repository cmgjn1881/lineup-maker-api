package com.lineupmaker.player.dto;

import com.lineupmaker.player.entity.Player;
import lombok.Getter;

@Getter
public class PlayerResponse {
    private final Long playerId;
    private final Long teamId;
    private final String name;
    private final String position;
    private final Integer number;

    public PlayerResponse(Player player) {
        this.playerId = player.getPlayerId();
        this.teamId = player.getTeam().getTeamId(); // Team Entity에서 ID 추출
        this.name = player.getName();
        this.position = player.getPosition();
        this.number = player.getBackNumber();
    }
}
