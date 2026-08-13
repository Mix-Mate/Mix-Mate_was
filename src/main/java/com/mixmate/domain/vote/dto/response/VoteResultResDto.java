package com.mixmate.domain.vote.dto.response;

import com.mixmate.domain.participant.dto.ParticipantSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteResultResDto {

    @Schema(description = "동점자가 있으면 여러 명이 담긴 공동 MVP 목록")
    private final List<MvpWinnerResDto> mvpWinners;

    @Schema(description = "2차까지 참여하는 참가자 목록")
    private final List<ParticipantSummary> secondRoundParticipants;
}
