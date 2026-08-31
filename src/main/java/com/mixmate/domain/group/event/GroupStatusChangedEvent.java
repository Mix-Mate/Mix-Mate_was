package com.mixmate.domain.group.event;

import com.mixmate.domain.group.enums.GroupStatus;

public record GroupStatusChangedEvent(
        Long groupId,
        GroupStatus status
) {
}
