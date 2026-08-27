package com.mixmate.domain.group.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * 그룹 참여코드로 쓸 8자리 무작위 문자열을 생성합니다.
 * 혼동되기 쉬운 문자(0/O, 1/I/l)는 문자셋에서 미리 제외했습니다.
 */
@Component
public class InviteCodeGenerator {

    // 혼동되는 문자 제외: 0/O, 1/I/l
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 6;

    private final SecureRandom random = new SecureRandom();

    /**
     * 참여코드 후보를 하나 생성합니다. 유일성(중복 여부)은 호출하는 쪽에서 확인해야 합니다.
     * @return 8자리 대문자/숫자 조합 문자열
     */
    public String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
