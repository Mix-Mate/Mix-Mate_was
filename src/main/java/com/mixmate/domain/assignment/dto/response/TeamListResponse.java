package com.mixmate.domain.assignment.dto.response;

import com.mixmate.domain.assignment.dto.TeamDetail;
import com.mixmate.domain.participant.enums.Round;

import java.util.List;

public record TeamListResponse(
        Round round,
        List<TeamDetail> teams
) {
}
