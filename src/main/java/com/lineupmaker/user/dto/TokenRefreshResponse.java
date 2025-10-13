package com.lineupmaker.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenRefreshResponse {

    private String accessToken;

    public TokenRefreshResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}
