package com.lineupmaker.auth.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    // 💡 프론트엔드 리다이렉트 URL (SuccessHandler와 동일한 Base URL 사용)
    private static final String FAILURE_REDIRECT_URI = "http://localhost:5173/#/oauth/redirect";

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        // 1. 에러 메시지 추출
        String errorMessage = exception.getLocalizedMessage() != null ? exception.getLocalizedMessage() : "소셜 로그인 중 알 수 없는 오류 발생";

        // 2. 에러 메시지를 쿼리 파라미터에 담아 프론트엔드로 리다이렉트
        String targetUrl = UriComponentsBuilder.fromUriString(FAILURE_REDIRECT_URI)
                .queryParam("error", "social_login_failed") // 오류 플래그
                .queryParam("message", errorMessage) // 상세 메시지
                .build().toUriString();

        // 3. SimpleUrlAuthenticationFailureHandler의 기본 로직을 사용하여 리다이렉트
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
