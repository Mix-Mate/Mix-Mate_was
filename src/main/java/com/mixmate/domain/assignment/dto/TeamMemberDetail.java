package com.mixmate.domain.assignment.dto;

import com.mixmate.domain.assignment.entity.TeamAssignmentMember;
import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.entity.ParticipantProfile;
import com.mixmate.domain.participant.enums.Visibility;

public record TeamMemberDetail(
        Long participantId,
        String displayName,
        String major,
        Visibility visibility,
        boolean fixed
) {
    public static TeamMemberDetail from(TeamAssignmentMember member) {
        Participant participant = member.getParticipant();
        ParticipantProfile profile = participant.getProfile();
        return new TeamMemberDetail(
                participant.getParticipantId(),
                profile.getDisplayName(),
                profile.getMajor(),
                profile.getVisibility(),
                member.isFixed()
        );
    }
}
