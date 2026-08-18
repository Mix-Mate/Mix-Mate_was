package com.mixmate.domain.participant.dto;

import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.entity.ParticipantProfile;
import com.mixmate.domain.participant.enums.Gender;
import com.mixmate.domain.participant.enums.Visibility;

public record ParticipantSummary(
        Long participantId,
        String displayName,
        String major,
        Gender gender,
        Visibility visibility
) {
    public static ParticipantSummary from(Participant participant) {
        ParticipantProfile profile = participant.getProfile();
        return new ParticipantSummary(participant.getParticipantId(), profile.getDisplayName(), profile.getMajor(), profile.getGender(), profile.getVisibility());
    }
}
