package com.lineupmaker.formation.entity;

import com.lineupmaker.team.entity.Team;
import com.lineupmaker.user.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "formation")
public class Formation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "formation_id")
    private Long formationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user; // 포메이션 생성 사용자 (FK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team; // 대상 팀 (FK)

    @Column(nullable = false, length = 100)
    private String name; // 포메이션 이름 (예: 4-4-2)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public Formation(Users user, Team team, String name) {
        this.user = user;
        this.team = team;
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    public void updateName(String name) {
        if (name != null) {
            this.name = name;
        }
    }
}
