package com.mixmate.domain.group.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GroupUpdateRequest(
        @NotBlank(message = "그룹 이름을 입력해주세요.")
        String groupName,

        String description
) {

}
