package com.mixmate.domain.assignment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FixedMember(

        @NotNull(message = "참가자를 선택해주세요.")
        Long participantId,

        @NotNull(message = "고정할 조를 선택해주세요.")
        @Min(value = 1, message = "조 번호는 1 이상이어야 합니다.")
        Integer teamNumber
) {
}
