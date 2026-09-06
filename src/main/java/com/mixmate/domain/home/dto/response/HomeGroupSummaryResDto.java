package com.mixmate.domain.home.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.group.entity.GroupBan;
import com.mixmate.domain.group.enums.GroupStatus;
import com.mixmate.domain.participant.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * state=active/finished면 status·memberCount·role이, state=banned면 reason·bannedAt이 채워진다.
 * 서로 배타적이라 상태별로 안 쓰는 필드는 응답에서 아예 빠지도록 null은 직렬화하지 않는다.
 */
@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomeGroupSummaryResDto {

    @Schema(example = "1")
    private final Long groupId;

    @Schema(example = "2026 SW 동아리 MT")
    private final String groupName;

    @Schema(description = "그룹 진행 상태 (state=banned에는 없음)", example = "RECRUITING")
    private final GroupStatus status;

    @Schema(description = "이 그룹의 전체 참여 인원수 (state=banned에는 없음)", example = "12")
    private final Long memberCount;

    @Schema(description = "요청자 본인의 이 그룹에서의 역할 (state=banned에는 없음)", example = "HOST")
    private final Role role;

    @Schema(description = "차단 사유 (state=banned에만 있음)", example = "부적절한 언행")
    private final String reason;

    @Schema(description = "차단된 시각 (state=banned에만 있음)", example = "2026-09-01T21:00:00")
    private final LocalDateTime bannedAt;

    public static HomeGroupSummaryResDto fromEntity(Group group, Role role, long memberCount) {
        return HomeGroupSummaryResDto.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .status(group.getStatus())
                .memberCount(memberCount)
                .role(role)
                .build();
    }

    public static HomeGroupSummaryResDto fromBan(GroupBan ban) {
        return HomeGroupSummaryResDto.builder()
                .groupId(ban.getGroup().getGroupId())
                .groupName(ban.getGroup().getGroupName())
                .reason(ban.getReason())
                .bannedAt(ban.getCreatedAt())
                .build();
    }
}
