package com.lineupmaker.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String email;
    private String username;
    // 필요한 경우 다른 사용자 정보 추가 가능

    public LoginResponse(String accessToken, String refreshToken, String email, String username) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.email = email;
        this.username = username;
    }
}
