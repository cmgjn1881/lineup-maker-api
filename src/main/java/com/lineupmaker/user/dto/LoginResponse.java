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
    // 필요한 경우 다른 사용자 정보 추가 가능

    public LoginResponse(String accessToken, String refreshToken, UUID userId, String username) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.username = username;
    }
}
