package com.lineupmaker.player.entity;

import com.lineupmaker.team.entity.Team;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "player")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_id")
    private Long playerId;

    // ManyToOne: N명의 선수가 1개의 팀에 속함
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team; // 팀 엔티티 참조 (team_id 외래키)

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20) // 포지션 정보 (e.g., "FW", "MF")
    private String position;

    @Column(name = "back_number", nullable = false)
    private Integer backNumber; // 등번호

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public Player(Team team, String name, String position, Integer backNumber) {
        this.team = team;
        this.name = name;
        this.position = position;
        this.backNumber = backNumber;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 선수 정보 수정 (Dirty Checking 활용)
     */
    public void updateDetails(String name, String position, Integer backNumber) {
        if (name != null) {
            this.name = name;
        }
        if (position != null) {
            this.position = position;
        }
        if (backNumber != null) {
            this.backNumber = backNumber;
        }
        // created_at은 변경하지 않음
    }
}
