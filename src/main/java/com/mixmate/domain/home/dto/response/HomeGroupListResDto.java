package com.mixmate.domain.home.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class HomeGroupListResDto {

    @Schema(description = "참여 그룹이 없으면 빈 배열")
    private final List<HomeGroupSummaryResDto> groups;
}
