package com.lineupmaker.user.controller;


import com.lineupmaker.user.dto.*;
import com.lineupmaker.user.jwt.JwtTokenProvider;
import com.lineupmaker.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;

    public AuthController(UserService userService, JwtTokenProvider tokenProvider) {
        this.userService = userService;
        this.tokenProvider = tokenProvider;
    }

    // 💡 [새로운 API] Step 1: 이메일만 입력받아 인증 코드를 요청하고 발송
    @PostMapping("/send-code")
    public ResponseEntity<String> sendCode(@RequestBody EmailRequest request) {
        try {
            userService.sendVerificationCode(request.getEmail());
            return ResponseEntity.ok("인증 코드가 이메일로 발송되었습니다. 5분 내로 코드를 입력해주세요.");
        } catch (IllegalArgumentException e) {
            // 이미 가입된 이메일 등 오류
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            // 이메일 전송 실패
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // 2. 코드 검증 및 플래그 저장 (Step 2: POST /api/auth/verify-code)
    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyCode(@RequestBody CodeVerificationRequest request) {
        try {
            // Redis에 코드 검증 후, 최종 가입을 허용하는 플래그를 저장
            userService.verifyCodeAndSetFlag(request);
            return ResponseEntity.ok("이메일 인증이 완료되었습니다. 이제 회원가입을 계속 진행해주세요.");
        } catch (IllegalArgumentException e) {
            // 코드 불일치, 코드 만료 등 오류
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 회원가입 API (POST /api/auth/signup)
    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody SignUpRequest request) {
        try {
            userService.signUp(request);
            // 💡 [수정 완료] 인증 코드 검증 후 최종 DB 저장이 성공했으므로, 최종 성공 메시지를 반환합니다.
            return ResponseEntity.ok("회원가입이 최종 완료되었습니다! 이제 로그인할 수 있습니다.");
        } catch (IllegalArgumentException e) {
            // 코드 불일치, 코드 만료, 이메일 중복 등 오류
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            // 서버 오류 (예: Redis 연결 문제)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("회원가입 중 오류가 발생했습니다: " + e.getMessage());
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

    /**
     * 로그아웃 API (POST /api/auth/logout)
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequest request) {
        try {
            userService.logout(request.getRefreshToken(), request.getAccessToken());

            // [핵심 수정] 204 No Content를 반환하여 REST 표준을 따릅니다.
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            // 토큰 파싱 오류 등이 발생해도 400 Bad Request로 처리
            return ResponseEntity.badRequest().build(); // 204 대신 400 에러를 반환하여 실패를 알림
        }
    }

    /**
     * 회원 탈퇴 API (DELETE /api/auth/withdraw)
     * - Access Token을 통해 인증된 사용자만 접근 가능
     */
    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdraw(Authentication authentication, HttpServletRequest request) {

        // 1. Security Context에서 사용자 이메일 (Subject) 추출
        String userEmail = ((UserDetails) authentication.getPrincipal()).getUsername();
        String accessTokenValue = tokenProvider.resolveToken(request);

        // 2. 서비스 로직 호출 (탈퇴 및 데이터 정리)
        // Access Token이 없으면 탈퇴 처리만 진행하고 경고를 남김 (refresh token은 무효화됨)
        userService.withdrawUser(userEmail, Objects.requireNonNullElse(accessTokenValue, ""));

        // 3. 204 No Content 반환 (RESTful 표준: 리소스 성공적 삭제)
        return ResponseEntity.noContent().build();
    }
}
