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

    // 생성자 주입: JdbcTemplate 제거, RedisTemplate 유지
    public HealthCheckController(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Operation(summary = "애플리케이션 헬스 체크", description = "Redis 연결 상태를 확인하고, 서버를 깨웁니다.")
    @GetMapping("/healthz")
    public ResponseEntity<String> healthCheck(
            @Parameter(description = "헬스 체크를 위한 비밀 키", required = true, example = "your-secret-key")
            @RequestHeader(value = "X-Health-Check-Key", required = false) String secretKey) {

        // 1. 보안 검증: 비밀 키 확인
        if (secretKey == null || !expectedHealthCheckKey.equals(secretKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Health Check Token");
        }

        try {
            // 2. Redis 연결 확인 및 깨우기
            String redisPing = redisTemplate.getConnectionFactory().getConnection().ping();
            if (!"PONG".equalsIgnoreCase(redisPing)) {
                log.warn("Health check failed: Redis did not respond with PONG.");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Redis is not responding");
            }

            // 3. Redis 정상 응답
            return ResponseEntity.ok("OK - Redis is awake and healthy");

        } catch (Exception e) {
            // 예외는 로그로 상세히 기록
            log.error("Health check failed due to an internal error with Redis.", e);
            // 클라이언트에게는 일반적인 메시지 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error - An internal error occurred during the health check.");
        }
    }
}
