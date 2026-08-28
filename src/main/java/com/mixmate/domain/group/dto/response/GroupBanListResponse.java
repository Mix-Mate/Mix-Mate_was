package com.mixmate.domain.group.dto.response;

import com.mixmate.domain.group.dto.GroupBanSummary;

import java.util.List;

public record GroupBanListResponse(
        List<GroupBanSummary> banList
) {
}
