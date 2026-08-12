package com.mixmate.domain.home.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class HomeInviteCodeVerifyReqDto {

    @Schema(description = "그룹 생성 시 발급된 8자리 참여코드. 생성일로부터 3일간 유효", example = "BZRWXTHD")
    @NotBlank(message = "참여코드를 입력해주세요.")
    private final String inviteCode;
}
