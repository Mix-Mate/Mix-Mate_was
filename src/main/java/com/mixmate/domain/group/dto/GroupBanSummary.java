package com.mixmate.domain.group.dto;

import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.group.entity.GroupBan;

import java.time.LocalDateTime;

public record GroupBanSummary(
        Long userId,
        String displayName,
        String email,
        String reason,
        LocalDateTime bannedAt
) {
    public static GroupBanSummary from(GroupBan ban) {
        User user = ban.getUser();
        return new GroupBanSummary(
                user.getUserId(), ban.getDisplayName(), user.getEmail(), ban.getReason(), ban.getCreatedAt());
    }
}
