package com.mixmate.domain.participant.dto;

import java.util.List;

public record ParticipantListResponse(
        List<ParticipantSummary> participantList
) {
}
