package com.lineupmaker.formation.dto;

import com.lineupmaker.formation.entity.Formation;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class FormationListResponse {
    private final Long formationId;
    private final String name;
    private final LocalDateTime createdAt;

    /**
     * Formation 엔티티를 받아 DTO를 생성하는 생성자입니다.
     */
    public FormationListResponse(Formation formation) {
        this.formationId = formation.getFormationId();
        this.name = formation.getName();
        this.createdAt = formation.getCreatedAt();
        // 목록 조회에 필요한 최소 필드만 포함
    }
}
