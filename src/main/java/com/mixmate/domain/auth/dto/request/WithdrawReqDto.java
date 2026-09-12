package com.mixmate.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class WithdrawReqDto {

    // 카카오 등 소셜 로그인 계정은 비밀번호가 없어 본인 확인을 JWT 인증만으로 대신한다.
    // 그래서 여기서는 @NotBlank를 걸지 않고, 로컬 계정 여부는 AuthService에서 판단한다.
    @Schema(description = "본인 확인용 현재 비밀번호. 소셜 로그인 계정은 비워도 됨", example = "password123")
    private final String password;
}
