package com.lineupmaker.user.service;

import com.lineupmaker.user.entity.Users;
import com.lineupmaker.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. DB에서 이메일로 Users 엔티티를 찾음
        Users users = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 이메일의 사용자를 찾을 수 없습니다: " + email));

        // 2. Spring Security의 User 객체로 변환하여 반환
        // Users 엔티티의 비밀번호(암호화된 값)와 권한 정보를 Security에 전달
        return org.springframework.security.core.userdetails.User.builder()
                .username(users.getEmail())
                .password(users.getPassword()) // 암호화된 비밀번호
                // 권한 설정 (여기서는 간단하게 "ROLE_USER" 권한을 부여)
                .roles("USER")
                .build();
    }
}
