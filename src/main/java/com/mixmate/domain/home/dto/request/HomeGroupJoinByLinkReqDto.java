package com.mixmate.domain.home.dto.request;

import com.mixmate.domain.participant.dto.request.ParticipantProfileRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 초대 링크로 입장할 때 쓰는 요청입니다.
 * 그룹은 경로의 토큰이 지목하므로, 참여코드 없이 프로필만 받습니다.
 */
@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class HomeGroupJoinByLinkReqDto {

    @Schema(description = "이 그룹에서 사용할 본인 프로필")
    @NotNull(message = "프로필 정보를 입력해주세요.")
    @Valid
    private final ParticipantProfileRequest profile;
}
