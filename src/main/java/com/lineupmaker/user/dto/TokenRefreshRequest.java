package com.lineupmaker.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TokenRefreshRequest {
    private String refreshToken;
    private String oldAccessToken;
}
