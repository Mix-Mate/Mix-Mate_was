package com.mixmate.domain.auth.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.mixmate.exception.CustomException;
import com.mixmate.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 카카오 로그인의 "인가 코드 → 액세스 토큰 → 사용자 정보" 두 단계를 대신 처리한다.
 *
 * Spring Security의 oauth2Login() 대신 이 방식을 쓰는 이유: oauth2Login()은 서버가 세션을 들고
 * 리다이렉트를 주도하는 흐름이라, 프론트(Vercel)와 백엔드(EC2) 도메인이 분리되어 있고 이미
 * 커스텀 JWT + 쿠키/헤더 이중화로 모바일 크로스도메인 문제를 우회하고 있는 이 프로젝트 구조와 맞지
 * 않는다. 대신 프론트가 카카오 인가 코드를 직접 백엔드로 던져주면, 여기서 그 코드를 카카오 서버와
 * 주고받아 최종 사용자 정보만 뽑아내고, 이후 토큰 발급은 기존 로그인 로직(AuthService)을 그대로 탄다.
 */
@Slf4j
@Component
public class KakaoApiClient {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient = RestClient.create();

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret:}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    public KakaoUserInfo getUserInfo(String code) {
        String kakaoAccessToken = requestAccessToken(code);
        return requestUserInfo(kakaoAccessToken);
    }

    private String requestAccessToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);
        if (StringUtils.hasText(clientSecret)) {
            body.add("client_secret", clientSecret);
        }

        JsonNode response;
        try {
            response = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            // 인가 코드는 1회용이라, 이미 쓴 코드나 만료된 코드로 재시도하면 여기서 항상 실패한다.
            log.warn("카카오 토큰 발급 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.OAUTH_LOGIN_FAILED);
        }

        if (response == null || !response.hasNonNull("access_token")) {
            throw new CustomException(ErrorCode.OAUTH_LOGIN_FAILED);
        }
        return response.get("access_token").asText();
    }

    private KakaoUserInfo requestUserInfo(String kakaoAccessToken) {
        JsonNode response;
        try {
            response = restClient.get()
                    .uri(USER_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            log.warn("카카오 사용자 정보 조회 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.OAUTH_LOGIN_FAILED);
        }

        if (response == null || !response.hasNonNull("id")) {
            throw new CustomException(ErrorCode.OAUTH_LOGIN_FAILED);
        }

        String providerId = response.get("id").asText();
        JsonNode account = response.path("kakao_account");
        String email = account.path("email").asText(null);
        String nickname = account.path("profile").path("nickname").asText(null);

        return new KakaoUserInfo(providerId, email, nickname);
    }
}
