package com.lineupmaker.user.controller;

import com.lineupmaker.user.dto.*;
import com.lineupmaker.user.entity.Users;
import com.lineupmaker.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignUpRequest request) {
        userService.signup(request);
        return ResponseEntity.ok("회원가입이 성공적으로 완료되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    public ResponseEntity<InfoResponse> getUserInfo(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        Users user = userService.findById(userId);

        InfoResponse response = InfoResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refresh(HttpServletRequest request) {
        String oldAccessToken = request.getHeader("X-Access-Token");
        String refreshToken = request.getHeader("X-Refresh-Token");
        String newAccessToken = userService.refreshAccessToken(refreshToken, oldAccessToken);
        return ResponseEntity.ok(newAccessToken);
    }

    /**
     * ✨ [NEW] 임시 토큰을 실제 토큰으로 교환하는 API 엔드포인트
     * @param payload 프론트엔드에서 보낸 임시 토큰을 담은 JSON 객체 (예: {"tempToken": "..."})
     * @return 실제 토큰 정보가 담긴 TokenBundle 객체
     */
    @PostMapping("/token/exchange")
    public ResponseEntity<TokenBundle> exchangeToken(@RequestBody Map<String, String> payload) {
        String tempToken = payload.get("tempToken");
        if (tempToken == null || tempToken.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        TokenBundle tokenBundle = userService.exchangeTempToken(tempToken);
        return ResponseEntity.ok(tokenBundle);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String accessToken = request.getHeader("Authorization").substring(7);
        userService.logout(accessToken);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody WithdrawRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        userService.withdraw(userId, request.getPassword());
        return ResponseEntity.noContent().build();
    }
}
