package com.lineupmaker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsWebConfig implements WebMvcConfigurer {

    // Vercel 도메인 목록
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            "http://localhost:5173",
            "https://lineup-frontend-nine.vercel.app",
            "https://lineup-frontend-git-develop-cmgjn1881s-projects.vercel.app",
            "https://lineup-frontend-87dma0lwz-cmgjn1881s-projects.vercel.app"
    );

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로에 대해 적용
                .allowedOrigins(ALLOWED_ORIGINS.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH") // 모든 필요한 메서드 허용
                .allowedHeaders("*") // 모든 헤더 허용
                .allowCredentials(true) // 인증 정보(쿠키, Authorization 헤더) 허용
                .maxAge(3600); // 1시간 캐시
    }
}
