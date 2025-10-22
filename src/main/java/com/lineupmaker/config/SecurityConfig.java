package com.lineupmaker.config;

import com.lineupmaker.user.jwt.JwtAuthenticationEntryPoint;
import com.lineupmaker.user.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 🔑 JwtAuthenticationEntryPoint를 주입받거나 내부에서 초기화해야 합니다.
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter
    , JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        // this.passwordEncoder 필드 초기화 구문이 없어졌습니다.
    }

    // Vercel 주소와 로컬 주소를 포함한 허용 출처 목록을 정의합니다.
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            "http://localhost:5173", // 로컬 개발 주소
            "https://lineup-frontend-nine.vercel.app/" // ⭐️ Vercel 배포 주소로 변경하세요!
            // 필요한 경우 Render 백엔드 자체 주소도 추가할 수 있습니다.
    );

    /**
     * CORS 설정을 위한 Bean 정의 (핵심 수정 부분)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 1. 허용할 출처(Origins) 설정
        configuration.setAllowedOrigins(ALLOWED_ORIGINS);

        // 2. 허용할 HTTP 메서드 설정 (GET, POST, PUT, DELETE, OPTIONS 등)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 3. 허용할 헤더 설정 (모두 허용)
        configuration.setAllowedHeaders(List.of("*"));

        // 4. 인증 정보 (쿠키, Authorization 헤더 등) 전송 허용
        configuration.setAllowCredentials(true);

        // 5. 캐시 시간 설정 (브라우저가 CORS 정보를 캐시하는 시간)
        configuration.setMaxAge(3600L); // 1시간

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 모든 경로(/**)에 대해 위에서 정의한 CORS 설정을 적용합니다.
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * 특정 경로에 대해 Security Filter Chain을 완전히 무시하도록 설정
     * CSRF 및 모든 보안 검사를 우회하여 403 문제를 해결합니다.
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                //"/api/auth/send-code",
                //"/api/auth/verify-code",
                "/api/auth/signup",
                "/api/auth/login",
                "/api/auth/refresh",
                "/api/auth/logout" // 로그아웃 경로를 Security 필터에서 완전히 제외
        );
    }

    // 2. HTTP 보안 필터 체인 설정 (핵심!)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                //CORS 비활성화 코드를 제거하고, 정의된 Bean을 사용해 CORS를 활성화합니다.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 사용 안 함 (JWT 등 Stateless 인증 방식을 위해)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 인증 실패 및 인가 실패 예외 처리
                .exceptionHandling(handling -> handling
                                // 🔑 인증 실패 (토큰 없음/만료/오류): 401 Unauthorized 반환
                                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        // .accessDeniedHandler(accessDeniedHandler) // 권한 부족 시 403 처리를 위한 핸들러 (선택 사항)
                )

                // 요청에 대한 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 나머지 모든 요청은 인증 필요 (Authenticated)
                        .anyRequest().authenticated()
                )
                // [핵심 추가] JWT 필터를 UsernamePasswordAuthenticationFilter 이전에 등록하여
                // 매 요청마다 토큰을 검증하게 합니다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
