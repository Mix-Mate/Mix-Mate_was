package com.mixmate.domain.group.dto.response;

import com.mixmate.domain.group.entity.Group;

import java.time.LocalDateTime;

public record GroupInviteResponse(
        String inviteCode,
        LocalDateTime expiresAt
) {
    public static GroupInviteResponse from(Group group) {
        return new GroupInviteResponse(group.getInviteCode(), group.getInviteExpiresAt());
    }
}
