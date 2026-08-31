package com.mixmate.domain.vote.dto.request;

import com.mixmate.domain.vote.enums.VoteChoice;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminRound2VoteReqDto {

    @Schema(description = "대신 투표할 대상 참가자 식별자. 로그인 계정이 없는(관리자가 대리 등록한) 참가자만 가능", example = "12")
    @NotNull(message = "투표 대상을 선택해주세요.")
    private final Long targetParticipantId;

    @Schema(description = "2차 참여 여부", example = "PARTICIPATE")
    @NotNull(message = "참여 여부를 선택해주세요.")
    private final VoteChoice choice;
}
