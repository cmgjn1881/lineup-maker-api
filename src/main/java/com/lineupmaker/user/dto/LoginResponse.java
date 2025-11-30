package com.lineupmaker.user.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private UUID userId;
    private String username;
    private String email; // email 필드 추가

    public LoginResponse(String accessToken, String refreshToken, UUID userId, String username, String email) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.username = username;
        this.email = email;
    }
}
