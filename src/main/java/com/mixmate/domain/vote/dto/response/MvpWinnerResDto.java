package com.mixmate.domain.vote.dto.response;

import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.enums.Grade;
import com.mixmate.domain.participant.enums.Mbti;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class MvpWinnerResDto {

    @Schema(example = "3")
    private final Long participantId;

    @Schema(example = "이서연")
    private final String displayName;

    @Schema(description = "1차 조 편성 정보가 없으면 null", example = "1")
    private final Integer teamNumber;

    @Schema(example = "FIRST")
    private final Grade grade;

    @Schema(example = "INFP")
    private final Mbti mbti;

    public static MvpWinnerResDto fromEntity(Participant participant, Integer teamNumber) {
        var profile = participant.getProfile();
        return MvpWinnerResDto.builder()
                .participantId(participant.getParticipantId())
                .displayName(profile.getDisplayName())
                .teamNumber(teamNumber)
                .grade(profile.getGrade())
                .mbti(profile.getMbti())
                .build();
    }
}
