package com.lineupmaker.user.service;

import com.lineupmaker.user.dto.LoginRequest;
import com.lineupmaker.user.dto.LoginResponse;
import com.lineupmaker.user.dto.SignUpRequest;
import com.lineupmaker.user.dto.TokenBundle;
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

    @Transactional
    public void signup(SignUpRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        Users newUser = Users.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .username(request.getUsername())
                .provider("local")
                .isVerified(true)
                .build();

        userRepository.save(newUser);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Users user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일을 찾을 수 없습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

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
    public String refreshAccessToken(String refreshTokenValue, String oldAccessToken) {
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

        if (StringUtils.hasText(oldAccessToken)) {
            Long remainingTime = tokenProvider.getRemainingExpirationTime(oldAccessToken);
            if (remainingTime > 0) {
                redisTemplate.opsForValue().set("blacklist:" + oldAccessToken, userId.toString(), remainingTime, TimeUnit.MILLISECONDS);
            }
        }

        Users user = findById(userId);
        return tokenProvider.createToken(user.getUserId().toString(), "USER");
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
                // 이 경우, 토큰은 어차피 더 이상 유효하지 않으므로 블랙리스트에 추가할 필요가 없습니다.
                // 로그아웃 요청은 성공적으로 처리된 것으로 간주합니다.
            }
        }
        // Refresh Token은 클라이언트에서 삭제하는 것을 전제로 하므로 서버에서는 별도 처리하지 않음
    }

    /**
     * ✨ [NEW] 임시 토큰을 실제 토큰 묶음으로 교환합니다.
     * @param tempToken 프론트엔드에서 받은 일회용 임시 토큰
     * @return Redis에 저장되어 있던 실제 토큰 묶음 (AccessToken, RefreshToken 등)
     */
    public TokenBundle exchangeTempToken(String tempToken) {
        // 1. Redis에서 임시 토큰을 사용하여 저장된 TokenBundle을 조회합니다.
        Object storedObject = redisTemplate.opsForValue().get(tempToken);

        if (storedObject == null) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 임시 토큰입니다.");
        }

        // 2. (중요) 한번 사용한 임시 토큰은 즉시 삭제하여 재사용을 방지합니다.
        redisTemplate.delete(tempToken);

        // 3. 조회된 객체를 TokenBundle 타입으로 변환하여 반환합니다.
        if (storedObject instanceof TokenBundle) {
            return (TokenBundle) storedObject;
        } else {
            // 예상치 못한 타입의 객체가 저장된 경우, 로깅하고 예외를 발생시킬 수 있습니다.
            throw new IllegalStateException("Redis에 예기치 않은 타입의 데이터가 저장되어 있습니다.");
        }
    }

    @Transactional
    public void withdraw(UUID userId, String password) {
        Users user = findById(userId);

        // 일반 로그인 사용자의 경우에만 비밀번호 확인
        if (user.getPassword() != null && !passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 연관된 RefreshToken 삭제
        refreshTokenRepository.deleteById(userId);

        // 사용자 삭제
        userRepository.delete(user);
    }

    public Users findById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }
}
