package com.mixmate.domain.assignment.dto.response;

import com.mixmate.domain.assignment.dto.TeamDetail;
import com.mixmate.domain.assignment.enums.AssignmentCondition;
import com.mixmate.domain.participant.enums.Round;

import java.util.List;
import java.util.Set;

public record TeamAssignmentResponse(
        Round round,
        int teamCount,
        Set<AssignmentCondition> conditions,
        List<TeamDetail> teams
) {
}
