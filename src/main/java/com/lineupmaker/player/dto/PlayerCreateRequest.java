package com.lineupmaker.player.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerCreateRequest {
    private String name;
    private String position;
    private Integer backNumber;
}
