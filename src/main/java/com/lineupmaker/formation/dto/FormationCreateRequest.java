package com.lineupmaker.formation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Schema(description = "포메이션 생성을 위한 요청 DTO")
public class FormationCreateRequest {
    @Schema(description = "포메이션이 속할 팀의 ID", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long teamId;

    @Schema(description = "포메이션 이름", example = "결승전 포메이션", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    // 이 목록에 여러 쿼터의 선수 배치가 포함될 수 있습니다.
    @Schema(description = "선수들의 포지션 및 쿼터 정보 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<PlayerPlacementDto> placements;

    // 쿼터별 심판 정보 (Key: 쿼터, Value: 심판 이름)
    @Schema(description = "쿼터별 심판 정보. Key는 쿼터(1~4), Value는 심판 이름입니다.",
            example = "{\"1\": \"홍길동\", \"2\": \"김철수\", \"3\": \"홍길동\", \"4\": \"이영희\"}",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<Integer, String> referees;
}
