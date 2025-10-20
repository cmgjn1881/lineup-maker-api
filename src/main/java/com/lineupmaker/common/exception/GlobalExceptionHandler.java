package com.lineupmaker.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 1. 403 Forbidden: 접근 권한 없는 예외 (Spring Security AccessDeniedException) 처리
     * - updateTeam, deleteTeam 등 팀 소유자 검증 실패 시 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN) // 403
                .body("접근이 거부되었습니다: " + ex.getMessage());
    }

    /**
     * 2. 404 Not Found: 리소스가 없는 경우 (예: 존재하지 않는 Team ID) 처리
     * - Service Layer에서 IllegalArgumentException을 던질 때 특정 키워드를 사용하여 분기 처리합니다.
     * - TeamService의 '존재하지 않는 팀 ID입니다'와 같은 메시지를 기준으로 판단한다고 가정합니다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {

        // 특정 리소스 부재 메시지 패턴 확인 (Service Layer에서 던진 예외를 기반으로 판단)
        if (ex.getMessage().contains("존재하지 않는 팀 ID") || ex.getMessage().contains("찾을 수 없습니다")) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND) // 404
                    .body(ex.getMessage());
        }

        // 그 외 일반적인 IllegalArgumentException은 400 Bad Request로 처리
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400
                .body(ex.getMessage());
    }
}
