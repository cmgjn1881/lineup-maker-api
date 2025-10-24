package com.lineupmaker.user.service;

import com.lineupmaker.auth.model.CustomPrincipal;
import com.lineupmaker.user.entity.Users;
import com.lineupmaker.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("CustomOAuth2UserService.loadUser() - 카카오 사용자 정보 로드 시작");

        // 1. 기본 OAuth2User 로드: 카카오로부터 사용자 정보를 받아옵니다.
        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.info("카카오 사용자 정보: {}", oAuth2User.getAttributes());

        // 2. Provider 정보 추출 ("kakao", "google" 등)
        String provider = userRequest.getClientRegistration().getRegistrationId(); // "kakao"

        // 3. 카카오 응답에서 핵심 필드 추출
        String providerId = oAuth2User.getName(); // 카카오의 고유 'id' (Long 타입이 String으로 변환됨)

        // attributes 맵에서 'properties' 객체 추출 후 닉네임 추출
        Map<String, Object> attributes = oAuth2User.getAttributes();
        Map<String, String> properties = (Map<String, String>) attributes.get("properties");

        // 닉네임 추출 (null 체크 포함)
        String nickname = (properties != null && properties.containsKey("nickname"))
                ? properties.get("nickname")
                : "소셜 사용자"; // 닉네임이 없을 경우 임시 값 사용
        log.info("추출된 사용자 정보 - provider: {}, providerId: {}, nickname: {}", provider, providerId, nickname);

        // 4. DB에 사용자 저장 또는 업데이트
        Users user = saveOrUpdate(provider, providerId, nickname);
        log.info("DB에 저장/업데이트된 사용자: {}", user);

        // 5. JWT 발급 및 인증 처리를 위해 CustomPrincipal 객체 반환
        return new CustomPrincipal(
                user.getUserId(),
                user.getUsername(),
                // 권한 정보 (예: 일반 사용자 권한 부여)
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                oAuth2User.getAttributes() // 기존 Attributes 정보 유지
        );
    }

    /**
     * 카카오 ID를 기반으로 사용자를 조회하거나 새로 생성합니다.
     */
    private Users saveOrUpdate(String provider, String providerId, String nickname) {
        // providerId를 기반으로 사용자 조회
        return userRepository.findByProviderAndProviderId(provider, providerId)
                .map(existingUser -> {
                    // ⭐️ [업데이트] 기존 사용자가 있다면 닉네임만 업데이트 (선택적)
                    log.info("기존 사용자 닉네임 업데이트: {}", nickname);
                    existingUser.updateUsername(nickname);
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    // ⭐️ [생성] 새로운 사용자 엔티티 생성 (email, password는 NULL)
                    log.info("신규 소셜 사용자 생성: {}", nickname);
                    return userRepository.save(Users.builder()
                            .provider(provider)
                            .providerId(providerId)
                            .username(nickname)
                            .email(null)
                            .password(null)
                            .isVerified(true) // 소셜 로그인은 인증된 것으로 간주
                            .build());
                });
    }
}
