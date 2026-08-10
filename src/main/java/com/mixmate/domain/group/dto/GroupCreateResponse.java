package com.mixmate.domain.group.dto;

import com.mixmate.domain.group.entity.Group;

public record GroupCreateResponse(
        Long groupId,
        String groupName,
        String inviteCode
) {
    public static GroupCreateResponse from(Group group) {
        return new GroupCreateResponse(group.getGroupId(), group.getGroupName(), group.getInviteCode());
    }
}
