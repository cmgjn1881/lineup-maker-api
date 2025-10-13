package com.lineupmaker.user.service;

import com.lineupmaker.user.dto.LoginRequest;
import com.lineupmaker.user.dto.LoginResponse;
import com.lineupmaker.user.dto.SignUpRequest;
import com.lineupmaker.user.entity.RefreshToken;
import com.lineupmaker.user.entity.Users;
import com.lineupmaker.user.jwt.JwtTokenProvider;
import com.lineupmaker.user.repository.RefreshTokenRepository;
import com.lineupmaker.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    // 회원가입
    @Transactional
    public Users signUp(SignUpRequest request) { // 반환 타입은 Users
        // 1. 이메일 중복 확인
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 2. 비밀번호 암호화 (핵심!)
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 3. Users Entity 생성
        Users newUser = Users.builder()
                .email(request.getEmail())
                .password(encodedPassword) // 암호화된 비밀번호 저장
                .username(request.getUsername())
                .build();

        // 4. DB에 저장 후 저장된 객체 반환
        return userRepository.save(newUser);
    }

    // [추가] 로그인 인증 테스트 메서드
    @Transactional
    public LoginResponse authenticate(LoginRequest request) {

        // 1. 이메일로 사용자 정보 조회
        Users users = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일을 찾을 수 없습니다."));

        // 2. 비밀번호 일치 확인
        if (!passwordEncoder.matches(request.getPassword(), users.getPassword())) {
            // 비밀번호 불일치 시 예외 발생
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 3. 인증 성공 시 Access Token 생성 (Role은 DB에서 가져와 사용)
        String accessToken = tokenProvider.createToken(users.getEmail(), "USER");

        // 4. [핵심] Refresh Token 생성 및 DB 관리
        String refreshTokenValue = tokenProvider.createRefreshToken();
        LocalDateTime expiresAt = tokenProvider.getRefreshTokenExpirationTime();

        // 기존 토큰이 있는지 확인 (있으면 업데이트, 없으면 새로 생성)
        refreshTokenRepository.findByUserId(users.getUserId()).ifPresentOrElse(
                // 기존 토큰이 있으면 값과 만료 시간 업데이트
                token -> token.updateTokenValue(refreshTokenValue, expiresAt),
                // 없으면 새로 생성하여 저장
                () -> {
                    RefreshToken newRefreshToken = RefreshToken.builder()
                            .userId(users.getUserId())
                            .tokenValue(refreshTokenValue)
                            .expiresAt(expiresAt)
                            .build();
                    refreshTokenRepository.save(newRefreshToken);
                }
        );

        // 5. 응답 DTO에 두 토큰을 담아 반환 (LoginResponse DTO 수정 필요)
        return new LoginResponse(accessToken, refreshTokenValue, users.getEmail());
    }

    public Users findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
