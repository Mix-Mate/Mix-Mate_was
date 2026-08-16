package com.mixmate.domain.assignment.dto.response;

import com.mixmate.domain.assignment.dto.TeamDetail;
import com.mixmate.domain.participant.enums.Round;

public record MyTeamResponse(
        Round round,
        TeamDetail team
) {
}
