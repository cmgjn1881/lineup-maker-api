package com.lineupmaker.user.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "users")
public class Users {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String username;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false; // 기본값 FALSE로 설정

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public Users(String email, String password, String username) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.createdAt = LocalDateTime.now();
    }
}
