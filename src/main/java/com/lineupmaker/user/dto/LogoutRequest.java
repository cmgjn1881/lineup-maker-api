package com.lineupmaker.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogoutRequest {
    // 로그아웃 시 무효화할 Refresh Token을 받습니다.
    private String refreshToken;
    private String accessToken; // [추가] 무효화할 Access Token
}
