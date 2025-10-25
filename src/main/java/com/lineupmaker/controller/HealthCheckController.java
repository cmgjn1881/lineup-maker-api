package com.lineupmaker.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class HealthCheckController {

    @Value("${app.health-check-key}")
    private String expectedHealthCheckKey;

    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    // 생성자 주입
    public HealthCheckController(JdbcTemplate jdbcTemplate, RedisTemplate<String, Object> redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/healthz")
    public ResponseEntity<String> healthCheck(
            @RequestHeader(value = "X-Health-Check-Key", required = false) String secretKey) {

        // 1. 보안 검증: 비밀 키 확인
        if (secretKey == null || !expectedHealthCheckKey.equals(secretKey)) {
            // 토큰이 없거나 일치하지 않으면 401 반환
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Health Check Token");
        }

        try {
            // 2. DB (PostgreSQL) 연결 확인 및 깨우기
            // 가장 가벼운 쿼리 실행 (DB 슬립 방지)
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);

            // 3. Redis 연결 확인 및 깨우기
            // PING 명령 실행 (Redis 슬립 방지)
            String redisPing = redisTemplate.getConnectionFactory().getConnection().ping();
            if (!"PONG".equalsIgnoreCase(redisPing)) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Redis is not responding");
            }

            // 4. 모든 서비스 정상 응답
            return ResponseEntity.ok("OK - All services are awake and healthy");

        } catch (Exception e) {
            // 예외는 로그로 상세히 기록
            log.error("Health check failed due to an internal error.", e);
            // 클라이언트에게는 일반적인 메시지 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error - An internal error occurred during the health check.");
        }
    }
}
