package com.lineupmaker.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor // ✨ [수정] JSON 역직렬화를 위해 기본 생성자를 추가합니다.
public class TokenBundle {
    private String accessToken;
    private String refreshToken;
    private String userId;
    private String username;
}
