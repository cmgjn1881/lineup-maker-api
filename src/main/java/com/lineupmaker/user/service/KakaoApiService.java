package com.lineupmaker.user.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class KakaoApiService {

    private final WebClient webClient;

    public KakaoApiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://kapi.kakao.com").build();
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

            @Getter
            public static class Profile {
                @JsonProperty("nickname")
                private String nickname;
            }
        }
        public String getNickname() {
            return kakaoAccount.getProfile().getNickname();
        }
    }
}
