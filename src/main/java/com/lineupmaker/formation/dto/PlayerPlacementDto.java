package com.lineupmaker.formation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerPlacementDto {
    private Long playerId;
    private Integer quarter; // 쿼터 (1~4)
    private Integer coordX;  // X 좌표 (0~1000)
    private Integer coordY;  // Y 좌표 (0~1000)
}
