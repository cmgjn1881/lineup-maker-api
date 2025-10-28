package com.lineupmaker.user.controller;

import com.lineupmaker.user.dto.*;
import com.lineupmaker.user.entity.Users;
import com.lineupmaker.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "인증")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignUpRequest request) {
        userService.signup(request);
        return ResponseEntity.ok("회원가입이 성공적으로 완료되었습니다.");
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "소셜 로그인 (카카오)")
    @PostMapping("/social-login")
    public ResponseEntity<LoginResponse> socialLogin(@RequestBody SocialLoginRequest request) {
        LoginResponse response = userService.socialLogin(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 정보 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/info")
    public ResponseEntity<InfoResponse> getUserInfo(@Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        Users user = userService.findById(userId);

        InfoResponse response = InfoResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Access Token 갱신", description = "Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.")
    @PostMapping("/refresh")
    public ResponseEntity<String> refresh(
            @Parameter(description = "만료된 Access Token (선택 사항)", in = ParameterIn.HEADER, schema = @Schema(type = "string"))
            @RequestHeader(value = "X-Access-Token", required = false) String oldAccessToken,
            @Parameter(description = "Access Token 갱신용 Refresh Token", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string"))
            @RequestHeader("X-Refresh-Token") String refreshToken) {

        String newAccessToken = userService.refreshAccessToken(refreshToken, oldAccessToken);
        return ResponseEntity.ok(newAccessToken);
    }

    @Operation(summary = "로그아웃")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(description = "로그아웃할 사용자의 Access Token", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "JWT"))
            @RequestHeader("Authorization") String authorizationHeader) {

        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            String accessToken = authorizationHeader.substring(7);
            userService.logout(accessToken);
        }
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "회원 탈퇴")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdraw(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) WithdrawRequest request) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        String password = (request != null) ? request.getPassword() : null;
        userService.withdraw(userId, password);
        return ResponseEntity.noContent().build();
    }
}
