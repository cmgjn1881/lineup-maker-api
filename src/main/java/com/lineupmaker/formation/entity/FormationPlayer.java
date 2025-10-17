package com.lineupmaker.formation.entity;

import com.lineupmaker.player.entity.Player;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "formation_player", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"formation_id", "player_id", "quarter"})
})
public class FormationPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fp_id")
    private Long fpId;

    @ManyToOne(fetch = FetchType.LAZY)@JoinColumn(name = "formation_id", nullable = false)
    private Formation formation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    private Integer quarter; // 적용 쿼터 (1~4)

    @Column(name = "coord_x", nullable = false)
    private Integer coordX; // X 좌표 (0~1000)

    @Column(name = "coord_y", nullable = false)
    private Integer coordY; // Y 좌표 (0~1000)

    @Builder
    public FormationPlayer(Formation formation, Player player, Integer quarter, Integer coordX, Integer coordY) {
        this.formation = formation;
        this.player = player;
        this.quarter = quarter;
        this.coordX = coordX;
        this.coordY = coordY;
    }
}
