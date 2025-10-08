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

import java.security.Key;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final Key key;
    private final long tokenValidityInMilliseconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.expiration}") long tokenValidityInSeconds) {

        // 시크릿 키를 Base64 디코딩하여 Key 객체 생성
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);

        // 초 단위를 밀리초 단위로 변환
        this.tokenValidityInMilliseconds = tokenValidityInSeconds * 1000;
    }

    /**
     * 사용자 ID와 권한 정보를 담아 Access Token을 생성합니다.
     */
    public String createToken(String subject, String role) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + this.tokenValidityInMilliseconds);

        return Jwts.builder()
                .setSubject(subject) // 토큰 주체 (사용자 ID 또는 이메일)
                .claim("role", role) // 사용자 권한 정보
                .signWith(key, SignatureAlgorithm.HS256) // HS256 알고리즘과 키로 서명
                .setExpiration(validity) // 만료 시간 설정
                .compact();
    }

    /**
     * 토큰의 유효성을 검증합니다.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(key) // 시크릿 키로 토큰 파싱 시도
                    .build()
                    .parseClaimsJws(token);
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
        Claims claims = Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

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
