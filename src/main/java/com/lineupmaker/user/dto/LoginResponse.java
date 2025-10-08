package com.lineupmaker.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private String token;
    private String email;
    // 필요한 경우 다른 사용자 정보 추가 가능

    public LoginResponse(String token, String email) {
        this.token = token;
        this.email = email;
    }
}
