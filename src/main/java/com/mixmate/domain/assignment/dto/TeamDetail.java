package com.mixmate.domain.assignment.dto;

import com.mixmate.domain.assignment.entity.TeamAssignmentMember;

import java.util.List;

public record TeamDetail(
        int teamNumber,
        List<TeamMemberDetail> members
) {
    public static TeamDetail of(int teamNumber, List<TeamAssignmentMember> members) {
        return new TeamDetail(
                teamNumber,
                members.stream().map(TeamMemberDetail::from).toList()
        );
    }
}
