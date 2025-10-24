package com.lineupmaker.auth.model; // 적절한 모델 패키지에 생성

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Getter
public class CustomPrincipal extends DefaultOAuth2User {

    private final UUID userId; // ⭐️ 우리 DB의 고유 ID (JWT에 포함될 핵심 값)
    private final String username; // 닉네임

    /**
     * @param userId 우리 DB의 Users.userId
     * @param username Users.username (닉네임)
     * @param authorities 사용자 권한
     * @param attributes 소셜 로그인 Attributes 맵
     */
    public CustomPrincipal(UUID userId, String username,
                           Collection<? extends GrantedAuthority> authorities,
                           Map<String, Object> attributes) {

        // 카카오 고유 ID('id') 필드를 name attribute key로 사용
        super(authorities, attributes, "id");
        this.userId = userId;
        this.username = username;
    }
}