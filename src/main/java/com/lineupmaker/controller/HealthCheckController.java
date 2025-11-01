package com.lineupmaker.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "헬스 체크")
@Slf4j
@RestController
public class HealthCheckController {

    @Value("${app.health-check-key}")
    private String expectedHealthCheckKey;

    private final RedisTemplate<String, Object> redisTemplate;

    public HealthCheckController(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Operation(summary = "애플리케이션 헬스 체크", description = "서버의 동작 여부를 확인합니다.")
    @GetMapping("/healthz")
    public ResponseEntity<String> healthCheck(
            @Parameter(description = "헬스 체크를 위한 비밀 키", required = true, example = "your-secret-key")
            @RequestHeader(value = "X-Health-Check-Key", required = false) String secretKey) {

        // 1. 보안 검증: 비밀 키 확인
        if (secretKey == null || !expectedHealthCheckKey.equals(secretKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Health Check Token");
        }

        /*
        // [주석 처리] Upstash는 잠자기 모드가 없으므로 '깨우기' 목적의 Redis PING은 불필요.
        // 추후 Redis 연결 상태를 직접 확인하고 싶을 경우 주석을 해제하여 사용 가능.
        try {
            // Redis 연결 확인 및 깨우기
            String redisPing = redisTemplate.getConnectionFactory().getConnection().ping();
            if (!"PONG".equalsIgnoreCase(redisPing)) {
                log.warn("Health check failed: Redis did not respond with PONG.");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Redis is not responding");
            }
        } catch (Exception e) {
            log.error("Health check failed due to an internal error with Redis.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error - An internal error occurred during the health check.");
        }
        */

        // 헬스 체크의 무게감을 낮추기 위해 서버 생존 여부만 신속하게 응답.
        return ResponseEntity.ok("OK");
    }
}
