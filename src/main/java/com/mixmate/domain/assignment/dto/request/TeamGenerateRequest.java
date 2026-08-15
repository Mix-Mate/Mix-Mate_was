package com.mixmate.domain.assignment.dto.request;

import com.mixmate.domain.assignment.dto.FixedMember;
import com.mixmate.domain.assignment.enums.AssignmentCondition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;

public record TeamGenerateRequest(

        @NotNull(message = "조 개수를 입력해주세요.")
        @Min(value = 2, message = "조는 2개 이상이어야 합니다.")
        Integer teamCount,

        @NotNull(message = "배치 조건을 입력해주세요.")
        Set<AssignmentCondition> conditions,

        @NotNull(message = "고정 멤버 목록을 입력해주세요.")
        @Valid
        List<FixedMember> fixedMembers
) {
}
