package com.mixmate.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * refreshToken 쿠키가 없는 클라이언트(쿠키를 못 쓰는 환경 등)를 위한 대안 경로.
 * 쿠키가 있으면 쿠키를 우선 사용하고, 이 바디는 쿠키가 없을 때만 본다.
 */
@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class TokenReissueReqDto {

    @Schema(description = "쿠키로 refreshToken을 보낼 수 없는 경우에만 사용", example = "618hdjfnvs3jr1f....")
    private final String refreshToken;
}
