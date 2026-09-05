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
public class WithdrawReqDto {

    @Schema(description = "본인 확인용 현재 비밀번호", example = "password123")
    @NotBlank(message = "비밀번호는 필수입니다.")
    private final String password;
}
