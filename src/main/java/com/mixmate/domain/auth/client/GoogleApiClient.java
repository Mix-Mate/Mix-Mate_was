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
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class GoogleApiClient {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final RestClient restClient = RestClient.create();

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    @Value("${google.redirect-uri}")
    private String redirectUri;

    public GoogleUserInfo getUserInfo(String code) {
        String googleAccessToken = requestAccessToken(code);
        return requestUserInfo(googleAccessToken);
    }

    private String requestAccessToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

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
            log.warn("구글 토큰 발급 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.OAUTH_LOGIN_FAILED);
        }

        if (response == null || !response.hasNonNull("access_token")) {
            throw new CustomException(ErrorCode.OAUTH_LOGIN_FAILED);
        }
        return response.get("access_token").asText();
    }

    private GoogleUserInfo requestUserInfo(String googleAccessToken) {
        JsonNode response;
        try {
            response = restClient.get().uri(USER_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + googleAccessToken)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            log.warn("구글 사용자 정보 조회 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.OAUTH_LOGIN_FAILED);
        }

        if (response == null || !response.hasNonNull("sub")) {
            throw new CustomException(ErrorCode.OAUTH_LOGIN_FAILED);
        }

        String providerId = response.get("sub").asText();
        String email = response.path("email").asText(null);
        String name = response.path("name").asText(null);

        return new GoogleUserInfo(providerId, email, name);
    }
}
