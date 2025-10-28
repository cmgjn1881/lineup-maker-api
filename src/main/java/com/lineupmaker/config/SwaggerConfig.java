package com.lineupmaker.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        tags = {
                @Tag(name = "인증", description = "사용자 인증 및 가입 관련 API"),
                @Tag(name = "팀", description = "팀 생성, 조회, 수정, 삭제 API"),
                @Tag(name = "선수", description = "팀 선수 생성, 조회, 수정, 삭제 API"),
                @Tag(name = "포메이션", description = "포메이션 생성, 조회, 수정, 삭제 API"),
                @Tag(name = "헬스 체크", description = "애플리케이션 및 의존성 상태 확인 API")
        }
)
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("Lineup Maker API")
                .version("v1.0")
                .description("라인업 메이커 프로젝트의 API 문서입니다.");

        // Bearer Token 인증 설정 (더 명확한 방식으로 수정)
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearerAuth", securityScheme))
                .info(info);
    }
}
