package com.lineupmaker.player.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerUpdateRequest {
    private String name;
    private String position;
    private Integer backNumber;
}
