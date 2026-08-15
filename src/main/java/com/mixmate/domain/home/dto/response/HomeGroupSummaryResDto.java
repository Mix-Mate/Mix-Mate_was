package com.mixmate.domain.home.dto.response;

import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.group.enums.GroupStatus;
import com.mixmate.domain.participant.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class HomeGroupSummaryResDto {

    @Schema(example = "1")
    private final Long groupId;

    @Schema(example = "2026 SW 동아리 MT")
    private final String groupName;

    @Schema(description = "그룹 진행 상태", example = "RECRUITING")
    private final GroupStatus status;

    @Schema(description = "이 그룹의 전체 참여 인원수", example = "12")
    private final long memberCount;

    @Schema(description = "요청자 본인의 이 그룹에서의 역할", example = "HOST")
    private final Role role;

    public static HomeGroupSummaryResDto fromEntity(Group group, Role role, long memberCount) {
        return HomeGroupSummaryResDto.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .status(group.getStatus())
                .memberCount(memberCount)
                .role(role)
                .build();
    }
}
