package com.lineupmaker.user.controller;


import com.lineupmaker.user.dto.*;
import com.lineupmaker.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // 회원가입 API (POST /api/auth/signup)
    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody SignUpRequest request) {
        try {
            userService.signUp(request);
            return ResponseEntity.ok("회원가입이 성공적으로 완료되었습니다.");
        } catch (IllegalArgumentException e) {
            // 이메일 중복 등 오류 발생 시 400 Bad Request 응답
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 로그인 API
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // Service에서 인증 및 토큰 생성까지 처리 후 LoginResponse 반환
            LoginResponse response = userService.authenticate(request);

            // 200 OK와 함께 토큰이 담긴 응답 반환
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // 이메일을 찾을 수 없는 경우
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/info")
    public ResponseEntity<String> getUSerInfo(Authentication authentication) {

        // 1. Security Context에서 현재 인증된 사용자 정보(Authentication)를 가져옵니다.
        // Spring Security는 JWT 필터가 저장한 UserDetails 객체를 여기에 넣어줍니다.
        if (authentication != null && authentication.isAuthenticated()) {

            // 2. UserDetails 객체에서 이메일(Subject)을 추출
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userEmail = userDetails.getUsername(); // JWT Subject (이메일)

            // 3. 응답 반환
            return ResponseEntity.ok("인증 성공! 현재 사용자 이메일: " + userEmail
                    + ", 권한: " + userDetails.getAuthorities());
        }

        // 여기에 도달할 일은 없지만, 안전을 위해 추가
        return ResponseEntity.status(401).body("인증되지 않은 사용자입니다.");
    }

    // Access Token 재발급 API
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestBody TokenRefreshRequest request) {
        try {
            String newAccessToken = userService.refreshAccessToken(
                    request.getRefreshToken(),
                    request.getOldAccessToken());

            // 새 Access Token을 응답 DTO에 담아 200 OK와 함께 반환
            TokenRefreshResponse response = new TokenRefreshResponse(newAccessToken);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // 유효하지 않은 리프레시 토큰일 경우 401 Unauthorized 반환
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}
