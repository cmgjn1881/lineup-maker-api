package com.lineupmaker.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

record ErrorResponse(int status, String message) {}

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        ErrorResponse response = new ErrorResponse(HttpStatus.FORBIDDEN.value(), "접근이 거부되었습니다: " + ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN) // 403
                .body(response); // 🔑 ErrorResponse DTO 반환
    }

    /**
     * 2. 404 Not Found 및 400 Bad Request 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {

        String message = ex.getMessage();

        // 404 Not Found 처리
        if (message.contains("존재하지 않는 팀 ID") || message.contains("찾을 수 없습니다")) {
            ErrorResponse response = new ErrorResponse(HttpStatus.NOT_FOUND.value(), message);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND) // 404
                    .body(response); // 🔑 ErrorResponse DTO 반환
        }

        // 🔑 400 Bad Request 처리 (등번호 중복, 유효성 검사 실패 등)
        // message는 "이미 사용중인 등번호입니다."를 포함합니다.
        ErrorResponse response = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400
                .body(response); // 🔑 ErrorResponse DTO 반환
    }
}
