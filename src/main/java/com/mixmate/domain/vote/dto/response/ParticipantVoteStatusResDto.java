package com.mixmate.domain.vote.dto.response;

import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.vote.enums.VoteChoice;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ParticipantVoteStatusResDto {

    @Schema(example = "3")
    private final Long participantId;

    @Schema(example = "이서연")
    private final String displayName;

    @Schema(description = "아직 투표하지 않았으면 null", example = "PARTICIPATE")
    private final VoteChoice choice;

    public static ParticipantVoteStatusResDto fromEntity(Participant participant, VoteChoice choice) {
        return ParticipantVoteStatusResDto.builder()
                .participantId(participant.getParticipantId())
                .displayName(participant.getProfile().getDisplayName())
                .choice(choice)
                .build();
    }
}
