package com.mixmate.domain.group.service;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * 초대 링크에 실을 22자 무작위 토큰을 생성합니다.
 * 사람이 손으로 입력하지 않는 값이라, 무작위 대입이 불가능하도록 참여코드보다 길게 만듭니다.
 */
@Component
public class InviteTokenGenerator {

    // 16바이트를 패딩 없는 base64url로 인코딩하면 정확히 22자가 된다
    private static final int BYTE_LENGTH = 16;
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final SecureRandom random = new SecureRandom();

    /**
     * 초대 링크 토큰을 하나 생성합니다. 128비트 난수라 중복 확인은 하지 않습니다.
     * @return URL에 그대로 쓸 수 있는 22자 문자열
     */
    public String generate() {
        byte[] bytes = new byte[BYTE_LENGTH];
        random.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }
}
