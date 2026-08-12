package com.mixmate.domain.home.dto.request;

import com.mixmate.domain.participant.dto.request.ParticipantProfileRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class HomeGroupJoinReqDto {

    @Schema(description = "입장할 그룹의 참여코드. verify로 미리 확인한 코드를 그대로 사용", example = "BZRWXTHD")
    @NotBlank(message = "참여코드를 입력해주세요.")
    private final String inviteCode;

    @Schema(description = "이 그룹에서 사용할 본인 프로필")
    @NotNull(message = "프로필 정보를 입력해주세요.")
    @Valid
    private final ParticipantProfileRequest profile;
}
