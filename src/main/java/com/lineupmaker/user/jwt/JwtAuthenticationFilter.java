package com.lineupmaker.user.jwt;

import io.jsonwebtoken.Claims;
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
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // 토큰 검증이 필요 없는 경로를 지정합니다.
        // SecurityConfig의 permitAll() 경로와 일치해야 합니다.
        return path.startsWith("/api/auth/signup") ||
                path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/refresh") ||
                path.startsWith("/api/auth/logout");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Request Header에서 토큰 추출
        String jwt = jwtTokenProvider.resolveToken(request);

        // 2. 토큰 유효성 검사
        if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {

            // 3. 블랙리스트 확인 (무효화된 Access Token 차단)
            if (redisTemplate.hasKey("blacklist:" + jwt)) {
                // 블랙리스트에 존재하면 무효화된 토큰이므로 접근 거부
                SecurityContextHolder.clearContext();

                // 응답의 인코딩과 Content Type을 명시적으로 설정
                response.setContentType("application/json;charset=UTF-8"); // JSON 형식과 UTF-8 명시

                // 401을 반환하도록 명확히 설정합니다.
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("인증 실패: 이 토큰은 무효화되었습니다.");
                return;
            }

            // 4. 토큰 타입 검증 (Refresh Token 접근 차단)
            try {
                Claims claims = jwtTokenProvider.getAllClaims(jwt);
                String tokenType = claims.get("token_type", String.class);

                if (!"access".equals(tokenType)) {
                    // Access Token이 아니면 (즉, Refresh Token이면) 인증 거부
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("인증 실패: Access Token만 사용할 수 있습니다.");
                    return;
                }

                // 5. 유효한 Access Token일 경우, Authentication 객체를 Security Context에 저장
                Authentication authentication = jwtTokenProvider.getAuthentication(jwt);
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                // 토큰 파싱 또는 클레임 추출 중 오류 발생 시
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("인증 실패: 토큰 구조가 유효하지 않습니다.");
                return;
            }
        }
        // 6. 다음 필터로 진행
        filterChain.doFilter(request, response);
    }
}
