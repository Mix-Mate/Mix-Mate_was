package com.mixmate.domain.participant.dto.response;

import com.mixmate.domain.participant.dto.ParticipantSummary;

import java.util.List;

public record ParticipantListResponse(
        List<ParticipantSummary> participantList
) {
}
