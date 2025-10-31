package com.lineupmaker.user.service;

import com.lineupmaker.user.dto.*;
import com.lineupmaker.user.entity.RefreshToken;
import com.lineupmaker.user.entity.Users;
import com.lineupmaker.user.jwt.JwtTokenProvider;
import com.lineupmaker.user.repository.RefreshTokenRepository;
import com.lineupmaker.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KakaoApiService kakaoApiService;

    @Transactional
    public void signup(SignUpRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresentOrElse(
                existingUser -> {
                    if ("WITHDRAWN".equals(existingUser.getStatus())) {
                        existingUser.activate();
                        existingUser.updatePassword(passwordEncoder.encode(request.getPassword()));
                        existingUser.updateUsername(request.getUsername());
                        userRepository.save(existingUser);
                    } else {
                        throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
                    }
                },
                () -> {
                    Users newUser = Users.builder()
                            .email(request.getEmail())
                            .password(passwordEncoder.encode(request.getPassword()))
                            .username(request.getUsername())
                            .provider("local")
                            .isVerified(true)
                            .build();
                    userRepository.save(newUser);
                }
        );
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Users user = userRepository.findByEmailAndStatus(request.getEmail(), "ACTIVE")
                .orElseThrow(() -> new IllegalArgumentException("이메일을 찾을 수 없거나 비활성화된 계정입니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return createAndSaveTokens(user);
    }

    // 1. @Transactional 제거: 외부 API 호출을 트랜잭션 밖으로 분리
    public LoginResponse socialLogin(SocialLoginRequest request) {
        if (!"kakao".equalsIgnoreCase(request.getProvider())) {
            throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다.");
        }

        // 2. 외부 API 호출을 트랜잭션 시작 전에 수행
        KakaoApiService.KakaoUserInfo kakaoUserInfo = kakaoApiService.getUserInfo(request.getAccessToken());

        // 3. DB 작업만 트랜잭션 메소드로 위임
        return findOrCreateUserAndLogin(kakaoUserInfo);
    }

    // socialLogin의 DB 작업을 처리하는 새로운 트랜잭션 메소드
    @Transactional
    public LoginResponse findOrCreateUserAndLogin(KakaoApiService.KakaoUserInfo kakaoUserInfo) {
        String providerId = kakaoUserInfo.getId();

        Users user = userRepository.findByProviderAndProviderId("kakao", providerId)
                .map(existingUser -> {
                    if ("WITHDRAWN".equals(existingUser.getStatus())) {
                        existingUser.activate();
                    }
                    return existingUser;
                })
                .orElseGet(() -> {
                    Users newUser = Users.builder()
                            .username(kakaoUserInfo.getNickname())
                            .provider("kakao")
                            .providerId(providerId)
                            .isVerified(true)
                            .build();
                    return userRepository.save(newUser);
                });

        return createAndSaveTokens(user);
    }


    private LoginResponse createAndSaveTokens(Users user) {
        String userIdStr = user.getUserId().toString();
        String accessToken = tokenProvider.createToken(userIdStr, "USER");
        String refreshTokenValue = tokenProvider.createRefreshToken(userIdStr);

        saveRefreshToken(user.getUserId(), refreshTokenValue);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .userId(UUID.fromString(userIdStr))
                .username(user.getUsername())
                .build();
    }

    @Transactional
    public void saveRefreshToken(UUID userId, String tokenValue) {
        Long expiration = tokenProvider.getRefreshTokenExpirationSeconds();
        RefreshToken refreshToken = refreshTokenRepository.findById(userId)
                .orElse(new RefreshToken(userId, tokenValue, expiration));

        refreshToken.updateTokenValue(tokenValue, expiration);
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public TokenRefreshResponse refreshAccessToken(String refreshTokenValue) {
        if (!tokenProvider.validateToken(refreshTokenValue)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 Refresh Token입니다.");
        }

        UUID userId = UUID.fromString(tokenProvider.getSubject(refreshTokenValue));
        RefreshToken storedToken = refreshTokenRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 Refresh Token입니다."));

        if (!storedToken.getRefreshToken().equals(refreshTokenValue)) {
            refreshTokenRepository.deleteById(userId);
            throw new IllegalArgumentException("토큰 값이 일치하지 않습니다. (탈취 의심)");
        }

        // 새로운 Access Token과 Refresh Token 생성
        String newAccessToken = tokenProvider.createToken(userId.toString(), "USER");
        String newRefreshToken = tokenProvider.createRefreshToken(userId.toString());

        // 새로 생성된 Refresh Token을 DB에 저장 (덮어쓰기)
        saveRefreshToken(userId, newRefreshToken);

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Transactional
    public void logout(String accessTokenValue) {
        if (StringUtils.hasText(accessTokenValue)) {
            try {
                Long remainingTime = tokenProvider.getRemainingExpirationTime(accessTokenValue);
                if (remainingTime > 0) {
                    String userId = tokenProvider.getSubject(accessTokenValue);
                    redisTemplate.opsForValue().set("blacklist:" + accessTokenValue, userId, remainingTime, TimeUnit.MILLISECONDS);
                }
            } catch (Exception e) {
                // 토큰이 만료되었거나 유효하지 않은 경우 예외가 발생할 수 있습니다.
            }
        }
    }

    // 1. @Transactional 제거: 여러 트랜잭션과 외부 API 호출을 오케스트레이션
    public void withdraw(UUID userId, String password) {
        // 2. 첫 번째 트랜잭션: 사용자 정보 조회 및 검증, 외부 API 호출에 필요한 정보 반환
        String providerId = prepareWithdrawal(userId, password);

        // 3. 외부 API 호출 (트랜잭션 밖에서 수행)
        if (providerId != null) {
            kakaoApiService.unlinkUserWithAdminKey(providerId);
        }

        // 4. 두 번째 트랜잭션: 실제 DB 회원 탈퇴 처리
        completeWithdrawal(userId);
    }

    // 회원 탈퇴 준비를 위한 트랜잭션 메소드 (읽기 전용)
    @Transactional(readOnly = true)
    public String prepareWithdrawal(UUID userId, String password) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        if ("kakao".equals(user.getProvider())) {
            return user.getProviderId(); // 카카오 연동 해제를 위해 providerId 반환
        } else {
            if (user.getPassword() != null && !passwordEncoder.matches(password, user.getPassword())) {
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
            }
            return null; // 로컬 계정은 반환할 ID 없음
        }
    }

    // 실제 회원 탈퇴 처리를 위한 트랜잭션 메소드
    @Transactional
    public void completeWithdrawal(UUID userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        user.markAsWithdrawn();
        refreshTokenRepository.deleteById(userId);
    }

    public Users findById(UUID userId) {
        return userRepository.findActiveById(userId)
                .orElseThrow(() -> new IllegalArgumentException("활성화된 사용자를 찾을 수 없습니다: " + userId));
    }
}
