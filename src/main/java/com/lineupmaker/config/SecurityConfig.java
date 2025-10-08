package com.lineupmaker.config;

import com.lineupmaker.user.jwt.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        // this.passwordEncoder 필드 초기화 구문이 없어졌습니다.
    }

    // 2. HTTP 보안 필터 체인 설정 (핵심!)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS 및 CSRF 보호 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)

                // 세션 사용 안 함 (JWT 등 Stateless 인증 방식을 위해)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 요청에 대한 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // [핵심] 회원가입 API는 인증 없이 누구나 접근 가능하도록 허용 (Permit All)
                        .requestMatchers("/api/auth/signup", "/api/auth/login").permitAll()

                        // 나머지 모든 요청은 인증 필요 (Authenticated)
                        .anyRequest().authenticated()
                )
                // [핵심 추가] JWT 필터를 UsernamePasswordAuthenticationFilter 이전에 등록하여
                // 매 요청마다 토큰을 검증하게 합니다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
