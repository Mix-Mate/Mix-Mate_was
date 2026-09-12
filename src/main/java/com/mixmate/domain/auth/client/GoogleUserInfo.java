package com.mixmate.domain.auth.client;

/**
 * 구글 사용자 정보 API(/oauth2/v3/userinfo) 응답에서 로그인에 필요한 값만 추려낸 것.
 *
 * @param providerId 구글 고유 회원번호(sub) User.providerId로 저장되어 재로그인 시 식별자로 쓰인다.
 * @param email      구글 계정 이메일. 항상 내려줌
 * @param name      구글 계정 표시 이름
 */
public record GoogleUserInfo(String providerId, String email, String name) {
}
