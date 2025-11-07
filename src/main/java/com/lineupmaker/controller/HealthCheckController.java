package com.lineupmaker.controller;

import io.swagger.v3.oas.annotations.Hidden;
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

    @Operation(summary = "외부 헬스 체크 (슬립 방지용)", description = "서버의 동작 여부를 확인합니다. (토큰 필요)")
    @GetMapping("/healthz")
    public ResponseEntity<String> externalHealthCheck(
            @Parameter(description = "헬스 체크를 위한 비밀 키", required = true, example = "your-secret-key")
            @RequestHeader(value = "X-Health-Check-Key", required = false) String secretKey) {

        if (secretKey == null || !expectedHealthCheckKey.equals(secretKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Health Check Token");
        }

        return ResponseEntity.ok("OK");
    }

    @Hidden
    @GetMapping("/internal-healthz")
    public ResponseEntity<String> internalHealthCheck() {
        return ResponseEntity.ok("OK");
    }
}
