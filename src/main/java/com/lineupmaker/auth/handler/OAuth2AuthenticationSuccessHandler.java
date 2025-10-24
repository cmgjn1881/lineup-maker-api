package com.lineupmaker.auth.handler;

import com.lineupmaker.auth.model.CustomPrincipal;
import com.lineupmaker.user.dto.TokenBundle;
import com.lineupmaker.user.jwt.JwtTokenProvider;
import com.lineupmaker.user.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final String redirectUri;

    @Autowired
    public OAuth2AuthenticationSuccessHandler(JwtTokenProvider tokenProvider, UserService userService, RedisTemplate<String, Object> redisTemplate, @Value("${app.oauth2.redirect-uri}") String redirectUri) {
        this.tokenProvider = tokenProvider;
        this.userService = userService;
        this.redisTemplate = redisTemplate;
        this.redirectUri = redirectUri;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        UUID userId = principal.getUserId();
        String userNickname = principal.getUsername();
        String userIdStr = userId.toString();

        // 1. 최종 Access/Refresh 토큰 생성
        String accessToken = tokenProvider.createToken(userIdStr, "ROLE_USER");
        String refreshToken = tokenProvider.createRefreshToken(userIdStr);
        userService.saveRefreshToken(userId, refreshToken);

        // 2. (NEW ✨) 임시 교환 토큰 생성 (간단한 UUID 사용)
        String tempToken = UUID.randomUUID().toString();

        // 3. (NEW ✨) Redis에 최종 토큰들을 임시 토큰을 Key로 하여 저장 (3분 유효)
        TokenBundle tokenBundle = new TokenBundle(accessToken, refreshToken, userIdStr, userNickname);
        redisTemplate.opsForValue().set(tempToken, tokenBundle, 3, TimeUnit.MINUTES);
        log.info("임시 교환 토큰이 Redis에 저장되었습니다. Key: {}", tempToken);

        // 4. (NEW ✨) 프론트엔드로는 임시 토큰만 전달
        String queryParams = UriComponentsBuilder.newInstance()
                .queryParam("tempToken", tempToken)
                .build()
                .getQuery();

        String targetUrl = redirectUri + "/#/oauth/redirect?" + queryParams;

        log.info("보안 강화 리다이렉트: 임시 토큰을 프론트엔드로 전달합니다. URL: {}", targetUrl);
        response.sendRedirect(targetUrl);
    }
}
