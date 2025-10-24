package com.lineupmaker.config;

import com.lineupmaker.auth.handler.OAuth2AuthenticationFailureHandler;
import com.lineupmaker.auth.handler.OAuth2AuthenticationSuccessHandler;
import com.lineupmaker.user.jwt.JwtAuthenticationEntryPoint;
import com.lineupmaker.user.jwt.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private OAuth2UserService customOAuth2UserService;
    @Lazy
    @Autowired
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    // ✨ [추가] FailureHandler를 주입받습니다.
    @Autowired
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                "/favicon.ico",
                "/api/auth/signup",
                "/api/auth/login",
                "/api/auth/refresh",
                "/api/auth/logout"
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())

                .oauth2Login(oauth2 -> oauth2
                                .authorizationEndpoint(auth -> auth
                                        .baseUri("/oauth2/authorization")
                                        .authorizationRequestRepository(new HttpSessionOAuth2AuthorizationRequestRepository())
                                )
                                .redirectionEndpoint(redirection -> redirection
                                        .baseUri("/login/oauth2/code/*")
                                )
                                .userInfoEndpoint(userInfo -> userInfo
                                        .userService(customOAuth2UserService)
                                )
                                .successHandler(oAuth2AuthenticationSuccessHandler)
                                // ✨ [추가] 로그인 실패 시 동작할 핸들러를 등록합니다.
                                .failureHandler(oAuth2AuthenticationFailureHandler)
                )

                .exceptionHandling(handling -> handling
                                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
