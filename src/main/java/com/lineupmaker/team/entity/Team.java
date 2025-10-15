package com.lineupmaker.team.entity;

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
@Table(name = "team")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long teamId;

    // Users 엔티티와의 관계 설정 (owner_id 필드에 매핑)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", columnDefinition = "UUID", nullable = false)
    private Users owner;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public Team(Users owner, String name) {
        this.owner = owner;
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }
}
