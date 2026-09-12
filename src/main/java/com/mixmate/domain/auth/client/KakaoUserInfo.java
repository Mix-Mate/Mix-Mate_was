package com.mixmate.domain.auth.client;

/**
 * 카카오 사용자 정보 API(/v2/user/me) 응답에서 로그인에 필요한 값만 추려낸 것.
 *
 * @param providerId 카카오 고유 회원번호(id). User.providerId로 저장되어 재로그인 시 식별자로 쓰인다.
 * @param email      카카오 계정 이메일. 이메일 동의를 안 했으면 null.
 * @param nickname   카카오톡 프로필 닉네임.
 */
public record KakaoUserInfo(String providerId, String email, String nickname) {
}
