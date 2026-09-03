package com.mixmate.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class TokenReissueResDto {

    @Schema(example = "618hdjfnvs3jr1f....")
    private final String accessToken;

    public static TokenReissueResDto of(String accessToken) {
        return TokenReissueResDto.builder()
                .accessToken(accessToken)
                .build();
    }
}
