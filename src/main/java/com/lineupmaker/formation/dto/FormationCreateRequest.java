package com.lineupmaker.formation.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class FormationCreateRequest {
    private Long teamId;
    private String name; // 포메이션 이름 (예: 4-4-2)

    // 이 목록에 여러 쿼터의 선수 배치가 포함될 수 있습니다.
    private List<PlayerPlacementDto> placements;

    // 쿼터별 심판 정보 (Key: 쿼터, Value: 심판 이름)
    private Map<Integer, String> referees;
}
