package com.lineupmaker.user.service;

import com.lineupmaker.user.dto.CodeVerificationRequest;
import com.lineupmaker.user.dto.LoginRequest;
import com.lineupmaker.user.dto.LoginResponse;
import com.lineupmaker.user.dto.SignUpRequest;
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

import java.util.Random;
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
    private final RedisTemplate<String, Object> redisTemplate; // RedisTemplate 주입

    //private final EmailService emailService;

    private static final String EMAIL_CODE_PREFIX = "EMAIL_CODE:";

    // Redis Key Prefix (인증 코드를 임시 저장하는 키)
    private static final String VERIFIED_EMAIL_PREFIX = "VERIFIED_EMAIL:";
    //private static final long VERIFICATION_CODE_TTL_MINUTES = 5; // 코드 유효 시간 5분
    private static final long VERIFICATION_FLAG_TTL_MINUTES = 30;

    // 💡 6자리 인증 코드를 생성하는 유틸리티 메서드
//    private String generateVerificationCode() {
//        Random random = new Random();
//        int code = 100000 + random.nextInt(900000); // 100000 ~ 999999
//        return String.valueOf(code);
//    }

    /**
     * 💡 [새로운 API] Step 1: 인증 코드를 생성하고 이메일로 발송합니다. (DB 저장 X)
     */
//    @Transactional
//    public void sendVerificationCode(String email) {
//        // 1. 이미 최종 가입된 사용자인지 확인
//        if (userRepository.findByEmail(email).isPresent()) {
//            throw new IllegalArgumentException("이미 가입된 이메일입니다. 로그인해 주세요.");
//        }
//
//        // 2. 6자리 인증 코드 생성
//        String code = generateVerificationCode();
//        String redisCodeKey = EMAIL_CODE_PREFIX + email;
//
//        // 3. Redis에 코드 저장 (유효 시간 5분 설정)
//        redisTemplate.opsForValue().set(
//                redisCodeKey,
//                code,
//                VERIFICATION_CODE_TTL_MINUTES,
//                TimeUnit.MINUTES
//        );
//
//        // 4. 이메일 발송
//        try {
//            emailService.sendVerificationCodeEmail(email, code);
//        } catch (RuntimeException e) {
//            // 이메일 전송 실패 시 Redis의 임시 코드도 삭제하는 것이 안전할 수 있습니다.
//            redisTemplate.delete(redisCodeKey);
//            throw new RuntimeException("이메일 전송에 실패했습니다. 이메일 주소를 확인하거나 잠시 후 다시 시도해 주세요.", e);
//        }
//    }

//    @Transactional
//    public void verifyCodeAndSetFlag(CodeVerificationRequest request) {
//        String email = request.getEmail();
//        String code = request.getVerificationCode();
//        String redisCodeKey = EMAIL_CODE_PREFIX + email;
//        String redisFlagKey = VERIFIED_EMAIL_PREFIX + email;
//
//        if (userRepository.findByEmail(email).isPresent()) {
//            throw new IllegalArgumentException("이미 가입된 이메일입니다. 로그인해 주세요.");
//        }
//
//        Object storedCodeObject = redisTemplate.opsForValue().get(redisCodeKey);
//        if (storedCodeObject == null) {
//            throw new IllegalArgumentException("인증 코드가 만료되었거나 발송되지 않았습니다. 코드를 다시 요청해주세요.");
//        }
//        String storedCode = storedCodeObject.toString();
//
//        if (!storedCode.equals(code)) {
//            throw new IllegalArgumentException("인증 코드가 일치하지 않습니다.");
//        }
//
//        redisTemplate.delete(redisCodeKey);
//
//        redisTemplate.opsForValue().set(
//                redisFlagKey,
//                "verified",
//                VERIFICATION_FLAG_TTL_MINUTES,
//                TimeUnit.MINUTES
//        );
//    }


    /**
     * 💡 Step 2: 최종 회원가입 및 코드 검증
     * 인증 코드와 함께 모든 정보를 제출하여 DB에 최종 저장하는 단계입니다.
     */
    @Transactional
    public Users signUp(SignUpRequest request) { // 반환 타입은 Users

        String email = request.getEmail();
        String redisFlagKey = VERIFIED_EMAIL_PREFIX + email;

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다. 로그인해 주세요.");
        }

        // render로 인한 이메일 인증 보류
//        if (redisTemplate.opsForValue().get(redisFlagKey) == null) {
//            throw new IllegalArgumentException("이메일 인증이 완료되지 않았거나 인증 시간이 만료되었습니다. 다시 인증해 주세요.");
//        }

        // --- 인증 성공: 최종 DB 저장 및 Redis 코드 삭제 ---

        // 4. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 5. Users Entity 생성 및 저장 (isVerified = TRUE)
        Users newUser = Users.builder()
                .email(email)
                .password(encodedPassword)
                .username(request.getUsername())
                .build();

        newUser.completeVerification();

        Users savedUser = userRepository.save(newUser);

        // 6. Redis의 임시 코드 삭제
        // render로 인한 이메일 인증 보류
        //redisTemplate.delete(redisFlagKey);

        return savedUser;
    }

    // 로그인 (Access/Refresh Token 발급 및 Redis 갱신)
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

        // 💡 [핵심 추가] 3. 이메일 인증 여부 확인
        if (!users.getIsVerified()) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않은 계정입니다. 메일함을 확인하거나 재전송을 요청해주세요.");
        }

        // 4. 인증 성공 시 Access Token 생성 (Role은 DB에서 가져와 사용)
        String accessToken = tokenProvider.createToken(users.getEmail(), "USER");

        // 5. Refresh Token 생성 시 UUID 전달
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
        return new LoginResponse(accessToken, refreshTokenValue, users.getEmail(), users.getUsername());
    }

    /**
     * [재발급 로직] 유효한 Refresh Token을 사용하여 새 Access Token을 발급합니다.
     * 이전 Access Token을 블랙리스트에 등록합니다.
     */
    @Transactional
    public String refreshAccessToken(String refreshTokenValue, String oldAccessToken) {

        // 1. Refresh Token 유효성 검증
        if (!tokenProvider.validateToken(refreshTokenValue)) {
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

        // --- Access Token 블랙리스트 등록 로직 시작 ---

        // 4. [핵심] 기존 Access Token의 남은 유효 시간 계산
        Long remainingTime = tokenProvider.getRemainingExpirationTime(oldAccessToken);

        if (remainingTime > 0) {
            String userSubject = tokenProvider.getSubject(oldAccessToken);
            // 5. [블랙리스트 등록] Redis에 "blacklist:[토큰]"을 Key로 저장하고, 남은 시간만큼 TTL 설정
            // 해당 키가 TTL 만료 전까지는 유효성 검사에서 걸러집니다.
            redisTemplate.opsForValue().set(
                    "blacklist:" + oldAccessToken,
                    userSubject, // 블랙리스트 값은 사용자 ID
                    remainingTime,
                    TimeUnit.MILLISECONDS
            );
        }

        // 6. 사용자 정보 조회 및 새 Access Token 생성
        String userEmail = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다.")).getEmail();

        // 5. Redis의 TTL이 자동으로 갱신되므로 여기서는 추가 갱신 로직 생략 가능

        return tokenProvider.createToken(userEmail, "USER");
    }

    @Transactional
    public void logout(String refreshTokenValue, String accessTokenValue) {

        // 🔑 [안전성 강화] 모든 JWT 처리 전에 문자열 유효성 검사
        if (!StringUtils.hasText(accessTokenValue) && !StringUtils.hasText(refreshTokenValue)) {
            System.err.println("JWT 경고: 로그아웃 요청에 Access/Refresh Token이 모두 누락되었습니다.");
            return; // 토큰이 아예 없으면 즉시 종료
        }

        // 1. [Access Token 무효화] 블랙리스트 등록
        if (StringUtils.hasText(accessTokenValue)) {
            try {
                Long remainingTime = tokenProvider.getRemainingExpirationTime(accessTokenValue); // 남은 유효 시간 계산

                if (remainingTime > 0) {
                    // Access Token의 Subject(이메일) 추출
                    String userSubject = tokenProvider.getSubject(accessTokenValue);

                    // Redis에 블랙리스트 등록
                    redisTemplate.opsForValue().set(
                            "blacklist:" + accessTokenValue,
                            userSubject,
                            remainingTime,
                            TimeUnit.MILLISECONDS
                    );
                } else {
                    System.err.println("JWT 정보: Access Token이 이미 만료되어 블랙리스트 등록을 건너뜁니다.");
                }
            } catch (Exception e) {
                // 이 catch 블록은 이제 파싱 오류를 잡아 경고 로그를 남깁니다.
                System.err.println("JWT 경고: Access Token 블랙리스트 등록 중 파싱 오류. " + e.getMessage());
            }
        }

        // 2. [Refresh Token 삭제] Redis에서 Refresh Token 삭제
        if (StringUtils.hasText(refreshTokenValue)) {
            // 🔑 Refresh Token의 유효성 검사를 시도하기 전에 형식 오류를 catch 합니다.
            try {
                // 유효성 검사를 여기서 수행하고 실패하면 다음 로직을 건너뛸 수 있습니다.
                if (tokenProvider.validateToken(refreshTokenValue)) {
                    UUID userId = UUID.fromString(tokenProvider.getSubject(refreshTokenValue));
                    refreshTokenRepository.deleteById(userId);
                } else {
                    System.err.println("JWT 경고: 로그아웃 요청 Refresh Token이 유효하지 않습니다 (Redis 삭제 건너뜁니다).");
                }
            } catch (Exception e) {
                System.err.println("Redis 경고: Refresh Token 처리 중 오류 발생. " + e.getMessage());
            }
        }
    }

    @Transactional
    public void withdrawUser(String userEmail, String accessTokenValue) {

        // 1. 사용자 엔티티 조회 (삭제할 대상 확인)
        Users userToDelete = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("탈퇴할 사용자를 찾을 수 없습니다."));

        UUID userId = userToDelete.getUserId();

        // 2. Refresh Token 정리 (Redis에서 삭제)
        // ON DELETE CASCADE를 사용하지만, Redis는 수동으로 정리해야 함
        refreshTokenRepository.deleteById(userId);

        // 3. Access Token 무효화
        // 블랙리스트로 무효화 처리
        Long remainingTime = tokenProvider.getRemainingExpirationTime(accessTokenValue);

        if (remainingTime > 0) {
            String userSubject = tokenProvider.getSubject(accessTokenValue);
            // 5. [블랙리스트 등록] Redis에 "blacklist:[토큰]"을 Key로 저장하고, 남은 시간만큼 TTL 설정
            // 해당 키가 TTL 만료 전까지는 유효성 검사에서 걸러집니다.
            redisTemplate.opsForValue().set(
                    "blacklist:" + accessTokenValue,
                    userSubject, // 블랙리스트 값은 사용자 ID
                    remainingTime,
                    TimeUnit.MILLISECONDS
            );
        }


        // 4. DB 사용자 삭제
        userRepository.delete(userToDelete);
    }

    public Users findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
