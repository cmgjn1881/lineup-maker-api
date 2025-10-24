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

@Component
public class JwtTokenProvider {

    private final Key key;
    private final long tokenValidityInMilliseconds;
    private final long refreshTokenValidityInSeconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.expiration}") long tokenValidityInSeconds,
            @Value("${jwt.refresh-expiration}") long refreshTokenValidityInSeconds) {

        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.tokenValidityInMilliseconds = tokenValidityInSeconds * 1000;
        this.refreshTokenValidityInSeconds = refreshTokenValidityInSeconds;
    }

    private JwtParser createParser() {
        return Jwts.parser()
                .verifyWith((SecretKey) this.key)
                .build();
    }

    private Jws<Claims> parseToken(String token) {
        return createParser().parseSignedClaims(token);
    }

    public Claims getAllClaims(String token) {
        try {
            return parseToken(token).getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        } catch (Exception e) {
            throw new JwtException("토큰 파싱 중 오류 발생: " + e.getMessage());
        }
    }

    public String getSubject(String token) {
        return getAllClaims(token).getSubject();
    }

    public String createToken(String userId, String role) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + this.tokenValidityInMilliseconds);

        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .claim("token_type", "access")
                .signWith(this.key)
                .expiration(validity)
                .compact();
    }

    public String createRefreshToken(String userId) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + this.refreshTokenValidityInSeconds * 1000);

        return Jwts.builder()
                .subject(userId)
                .expiration(validity)
                .signWith(this.key)
                .compact();
    }

    public Long getRemainingExpirationTime(String token) {
        try {
            Claims claims = getAllClaims(token);
            Date expiration = claims.getExpiration();
            long remaining = expiration.getTime() - new Date().getTime();
            return Math.max(0L, remaining);
        } catch (Exception e) {
            return 0L;
        }
    }

    public Long getRefreshTokenExpirationSeconds() {
        return this.refreshTokenValidityInSeconds;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith((SecretKey) key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            System.err.println("Invalid JWT signature or format: " + e.getMessage());
            throw e;
        } catch (ExpiredJwtException e) {
            System.err.println("Expired JWT token: " + e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            System.err.println("Unsupported JWT token: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("JWT claims string is empty: " + e.getMessage());
        }
        return false;
    }

    public Authentication getAuthentication(String token) {
        Claims claims = getAllClaims(token);

        String role = claims.get("role", String.class);
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(authority);

        UserDetails principal = new User(claims.getSubject(), "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
