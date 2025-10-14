package com.lineupmaker.user.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final Key key;
    private final long tokenValidityInMilliseconds;
    private final long refreshTokenValidityInSeconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.expiration}") long tokenValidityInSeconds,
            @Value("${jwt.refresh-expiration}") long refreshTokenValidityInSeconds) {

        // 시크릿 키를 Base64 디코딩하여 Key 객체 생성
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);

        // 초 단위를 밀리초 단위로 변환
        this.tokenValidityInMilliseconds = tokenValidityInSeconds * 1000;

        // refresh_token 만료 시간 설정
        this.refreshTokenValidityInSeconds = refreshTokenValidityInSeconds;
    }

    private JwtParser createParser() {
        return Jwts.parser()
                .verifyWith((SecretKey) this.key) // Key를 SecretKey로 캐스팅하여 검증
                .build();
    }

    private Jws<Claims> parseToken(String token) {
        return createParser().parseSignedClaims(token); // parseSignedClaims 사용
    }

    /**
     * 토큰에서 모든 클레임을 안전하게 추출하는 공통 메서드
     */
    public Claims getAllClaims(String token) {
        try {
            return parseToken(token).getPayload();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰의 클레임 정보를 가져와서 남은 시간 계산에 사용
            return e.getClaims();
        } catch (Exception e) {
            // 서명 오류, 형식 오류 등 다른 예외 발생 시
            throw new JwtException("토큰 파싱 중 오류 발생: " + e.getMessage());
        }
    }

    public String getSubject(String token) {
        return getAllClaims(token).getSubject();
    }

    /**
     * 사용자 ID와 권한 정보를 담아 Access Token을 생성합니다.
     * Access Token 생성: 'token_type: access' 클레임 추가 (Refresh Token과 구별)
     */
    public String createToken(String subject, String role) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + this.tokenValidityInMilliseconds);

        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .claim("token_type", "access")
                .signWith(this.key) // Key 객체 사용
                .expiration(validity) // expiration() 사용
                .compact();
    }

    /**
     * Refresh Token 생성: 'token_type' 클레임은 추가하지 않습니다.
     */
    public String createRefreshToken(UUID userId) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + this.refreshTokenValidityInSeconds * 1000);

        // 리프레시 토큰에는 사용자 ID를 넣지 않고 고유 식별자(UUID)를 Subject로 사용하기도 하나,
        // 여기서는 DB에서 관리되므로, 간단하게 Subject를 비우거나 임의의 값을 사용합니다.
        return Jwts.builder()
                .subject(userId.toString())
                .expiration(validity)
                .signWith(this.key)
                .compact();
    }

    // Redis 블랙리스트 등록에 사용할 토큰의 남은 만료 시간을 가져오는 메서드
    public Long getRemainingExpirationTime(String token) {
        try {
            // parseToken 메서드를 사용하여 안정적으로 만료 시간을 추출
            Claims claims = getAllClaims(token);
            Date expiration = claims.getExpiration();

            // 현재 시간과의 차이를 계산하여 남은 시간 (밀리초) 반환
            // 음수일 경우 0을 반환하도록 처리
            long remaining = expiration.getTime() - new Date().getTime();
            return Math.max(0L, remaining);

        } catch (Exception e) {
            // 서명이 잘못된 토큰 등 유효하지 않은 토큰은 0 반환
            return 0L;
        }
    }

    /**
     * Refresh Token의 만료 시간(초)를 반환하는 메서드 (Redis TTL 사용)
     */
    public Long getRefreshTokenExpirationSeconds() {
        return this.refreshTokenValidityInSeconds;
    }

    /**
     * 토큰의 유효성을 검증합니다.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith((SecretKey) key) // 시크릿 키로 토큰 파싱 시도
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            // 잘못된 JWT 서명입니다.
            System.err.println("Invalid JWT signature: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            // 만료된 JWT 토큰입니다.
            System.err.println("Expired JWT token: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            // 지원되지 않는 JWT 토큰입니다.
            System.err.println("Unsupported JWT token: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            // JWT 토큰이 잘못되었습니다. (빈 문자열 등)
            System.err.println("JWT claims string is empty: " + e.getMessage());
        }
        return false;
    }

    /**
     * 토큰에서 사용자 인증 정보(Authentication)를 추출합니다.
     */
    public Authentication getAuthentication(String token) {
        Claims claims = getAllClaims(token);

        // 토큰에 저장된 권한 정보를 기반으로 GrantedAuthority 리스트 생성
        // 여기서는 "ROLE_USER" 권한을 가정합니다.
        String role = claims.get("role", String.class);
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(authority);

        // 토큰 주체(subject, 즉 이메일)와 권한 정보를 담아 Authentication 객체 생성
        // 비밀번호는 이미 인증된 상태이므로 빈 문자열을 사용합니다.
        UserDetails principal = new User(claims.getSubject(), "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    /**
     * Request Header에서 JWT 토큰을 추출합니다. (Bearer Type 기준)
     */
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer "를 제외한 토큰 문자열 반환
        }
        return null;
    }

}
