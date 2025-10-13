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

import java.util.UUID;

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

        // 4. Refresh Token 생성 시 UUID 전달
        String refreshTokenValue = tokenProvider.createRefreshToken(users.getUserId()); // [수정] UUID 전달
        Long expirationSeconds = tokenProvider.getRefreshTokenExpirationSeconds();

        // 기존 토큰이 있는지 확인 (있으면 업데이트, 없으면 새로 생성)
        refreshTokenRepository.findById(users.getUserId()).ifPresentOrElse(
                // 기존 토큰이 있으면 값과 만료 시간 업데이트
                token -> {
                    token.updateTokenValue(refreshTokenValue, expirationSeconds);
                    refreshTokenRepository.save(token); // 명시적으로 save 호출
                },
                // 없으면 새로 생성하여 저장
                () -> {
                    RefreshToken newRefreshToken = RefreshToken.builder()
                            .userId(users.getUserId())
                            .refreshToken(refreshTokenValue)
                            .expiration(expirationSeconds) // TTL 설정
                            .build();
                    refreshTokenRepository.save(newRefreshToken);
                }
        );

        // 5. 응답 DTO에 두 토큰을 담아 반환 (LoginResponse DTO 수정 필요)
        return new LoginResponse(accessToken, refreshTokenValue, users.getEmail());
    }

    /**
     * [재발급 로직] 유효한 Refresh Token을 사용하여 새 Access Token을 발급합니다.
     */
    @Transactional
    public String refreshAccessToken(String refreshTokenValue) {

        // 1. Refresh Token 유효성 검 (JWT 형식 검증 및 만료 여부)
        if (!tokenProvider.validateToken(refreshTokenValue)) {
            // 토큰이 위변조되었거나, JWT 만료 시간이 지났다면 실패(Redis TTL과 별개)
            throw new IllegalArgumentException("유효하지 않거나 만료된 Refresh Token입니다.");
        }

        // [핵심 변경] 토큰에서 user_id (UUID)를 추출합니다.
        UUID userId = UUID.fromString(tokenProvider.getSubject(refreshTokenValue));

        // 2. Redis에서 Key(UUID)를 사용하여 직접 조회
        // findById는 Key로 직접 조회하므로 가장 빠르고 확실합니다.
        RefreshToken storedToken = refreshTokenRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Redis에 등록되지 않은 Refresh Token입니다."));

        // 3. 토큰 값 일치 확인 (보안 강화)
        if (!storedToken.getRefreshToken().equals(refreshTokenValue)) {
            // [추가] DB에 저장된 값과 요청된 값이 다르면 실패 (토큰 탈취 시 유효)
            // [핵심] 만약 값이 다르면, 즉시 해당 사용자 계정의 토큰을 모두 무효화합니다.
            refreshTokenRepository.deleteById(userId); // DB에서 즉시 삭제
            throw new IllegalArgumentException("토큰 값이 일치하지 않습니다. (탈취 의심)");
        }


        // 4. 사용자 정보 조회 및 새 Access Token 생성
        String userEmail = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다.")).getEmail();

        String newAccessToken = tokenProvider.createToken(userEmail, "USER");

        // 5. Redis의 TTL이 자동으로 갱신되므로 여기서는 추가 갱신 로직 생략 가능

        return newAccessToken;
    }

    public Users findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
