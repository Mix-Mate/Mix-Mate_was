package com.mixmate.domain.group.dto.response;

import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.group.enums.GroupStatus;
import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.enums.Role;

public record GroupDetailResponse(
        Long groupId,
        String groupName,
        String description,
        GroupStatus status,
        String inviteCode,
        long memberCount,
        Role myRole,
        Long myParticipantId
) {
    public static GroupDetailResponse from(Participant me, long memberCount) {
        Group group = me.getGroup();
        return new GroupDetailResponse(
                group.getGroupId(),
                group.getGroupName(),
                group.getDescription(),
                group.getStatus(),
                group.getInviteCode(),
                memberCount,
                me.getRole(),
                me.getParticipantId()
        );
    }
}
