package com.mixmate.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class KakaoLoginReqDto {

    @Schema(description = "프론트가 카카오 인가 코드 응답에서 받은 code 파라미터", example = "a1b2c3d4e5...")
    @NotBlank(message = "인가 코드는 필수입니다.")
    private final String code;
}
