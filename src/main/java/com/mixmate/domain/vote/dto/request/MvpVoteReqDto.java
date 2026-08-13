package com.mixmate.domain.vote.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class MvpVoteReqDto {

    @Schema(description = "MVP로 투표할 같은 조원의 참가자 식별자", example = "3")
    @NotNull(message = "투표 대상을 선택해주세요.")
    private final Long targetParticipantId;
}
