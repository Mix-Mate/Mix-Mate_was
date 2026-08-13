package com.mixmate.domain.vote.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class Round2VoteStatusResDto {

    @Schema(description = "그룹 전체 참가자 수", example = "12")
    private final int totalParticipantCount;

    @Schema(description = "투표를 완료한 인원수", example = "10")
    private final int votedCount;

    @Schema(description = "참여를 선택한 인원수", example = "8")
    private final int participateCount;

    @Schema(description = "불참을 선택한 인원수", example = "2")
    private final int notParticipateCount;

    @Schema(description = "참가자별 투표 상태 목록")
    private final List<ParticipantVoteStatusResDto> participants;
}
