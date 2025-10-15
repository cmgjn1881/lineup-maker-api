package com.lineupmaker.team.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamUpdateRequest {
    private String newName;

    public String newName() {
        return newName;
    }
}
