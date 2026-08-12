package com.mixmate.domain.home.dto.response;

import com.mixmate.domain.group.entity.Group;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class HomeInviteCodeVerifyResDto {

    @Schema(example = "1")
    private final Long groupId;

    @Schema(example = "테스트 조모임")
    private final String groupName;

    public static HomeInviteCodeVerifyResDto fromEntity(Group group) {
        return HomeInviteCodeVerifyResDto.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .build();
    }
}
