package com.lineupmaker.user.service;

import com.lineupmaker.user.dto.LoginRequest;
import com.lineupmaker.user.dto.LoginResponse;
import com.lineupmaker.user.dto.SignUpRequest;
import com.lineupmaker.user.entity.Users;
import com.lineupmaker.user.jwt.JwtTokenProvider;
import com.lineupmaker.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

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
    public LoginResponse authenticate(LoginRequest request) {

        // 1. 이메일로 사용자 정보 조회
        Users users = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일을 찾을 수 없습니다."));

        // 2. 비밀번호 일치 확인
        if (!passwordEncoder.matches(request.getPassword(), users.getPassword())) {
            // 비밀번호 불일치 시 예외 발생
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 3. [핵심] 인증 성공 시 JWT 토큰 생성
        // 토큰 subject에 email을 사용하고, role은 임시로 "USER"로 설정
        String token = tokenProvider.createToken(users.getEmail(), "USER");

        // 4. 응답 DTO에 토큰을 담아 반환
        return new LoginResponse(token, users.getEmail());
    }

    public Users findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
