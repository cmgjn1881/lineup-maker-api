package com.lineupmaker.user.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Request Header에서 토큰 추출
        String jwt = jwtTokenProvider.resolveToken(request);

        // 2. 토큰 유효성 검사
        if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {

            // [핵심 추가] 3. 블랙리스트 확인
            // Redis에 "blacklist:[토큰]" 이라는 키가 있는지 확인
            if (redisTemplate.hasKey("blacklist:" + jwt)) {
                // 블랙리스트에 존재하면 무효화된 토큰이므로 접근 거부
                SecurityContextHolder.clearContext();

                // 401을 반환하도록 명확히 설정합니다.
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Unauthorized: This token has been invalidated.");
                return;
            }

            // 4. 유효한 토큰일 경우, Authentication 객체를 Security Context에 저장
            Authentication authentication = jwtTokenProvider.getAuthentication(jwt);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        // 4. 다음 필터로 진행
        filterChain.doFilter(request, response);
    }
}
