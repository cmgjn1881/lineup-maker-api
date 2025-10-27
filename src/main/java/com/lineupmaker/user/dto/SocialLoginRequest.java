package com.lineupmaker.user.dto;

import lombok.Getter;

@Getter
public class SocialLoginRequest {
    private String provider;
    private String accessToken;
}
