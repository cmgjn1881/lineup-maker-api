package com.lineupmaker.user.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class KakaoApiService {

    private final WebClient webClient;
    private final String kakaoAdminKey;

    public KakaoApiService(WebClient.Builder webClientBuilder, @Value("${kakao.admin.key}") String kakaoAdminKey) {
        this.webClient = webClientBuilder.baseUrl("https://kapi.kakao.com").build();
        this.kakaoAdminKey = kakaoAdminKey;
    }

    /**
     * 카카오 Access Token을 사용하여 사용자 정보를 가져옵니다.
     * @param accessToken 클라이언트로부터 받은 카카오 Access Token
     * @return 카카오 사용자 정보
     */
    public KakaoUserInfo getUserInfo(String accessToken) {
        try {
            return webClient.get()
                    .uri("/v2/user/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(KakaoUserInfo.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to get user info from Kakao.", e);
            throw new IllegalArgumentException("유효하지 않은 카카오 토큰입니다.");
        }
    }

    /**
     * [Admin Key] 카카오 사용자의 providerId를 사용하여 카카오 연결을 끊습니다 (Unlink).
     * @param providerId 우리 DB에 저장된 카카오 사용자의 고유 ID
     */
    public void unlinkUserWithAdminKey(String providerId) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("target_id_type", "user_id");
        formData.add("target_id", providerId);

        try {
            webClient.post()
                    .uri("/v1/user/unlink")
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoAdminKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("Kakao user unlinked successfully with admin key for providerId: {}", providerId);
        } catch (Exception e) {
            log.error("Failed to unlink user from Kakao with admin key for providerId: {}", providerId, e);
            // 연결 끊기 실패 시에도 우리 서비스 탈퇴는 진행될 수 있도록 예외를 다시 던지지 않음
        }
    }

    /**
     * 카카오 사용자 정보를 담는 내부 DTO
     */
    @Getter
    public static class KakaoUserInfo {
        @JsonProperty("id")
        private String id;

        @JsonProperty("kakao_account")
        private KakaoAccount kakaoAccount;

        @Getter
        public static class KakaoAccount {
            @JsonProperty("profile")
            private Profile profile;

            @JsonProperty("email")
            private String email;

            @Getter
            public static class Profile {
                @JsonProperty("nickname")
                private String nickname;
            }
        }
        public String getNickname() {
            return kakaoAccount.getProfile().getNickname();
        }

        public String getEmail() {
            if (kakaoAccount == null) {
                return null;
            }
            return kakaoAccount.getEmail();
        }
    }
}
