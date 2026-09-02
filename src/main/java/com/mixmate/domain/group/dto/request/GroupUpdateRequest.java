package com.mixmate.domain.group.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GroupUpdateRequest(
        @NotBlank(message = "그룹 이름을 입력해주세요.")
        @Pattern(regexp = "^[^<>]*$", message = "그룹 이름에 <, > 문자는 사용할 수 없습니다.")
        String groupName,

        @Pattern(regexp = "^[^<>]*$", message = "설명에 <, > 문자는 사용할 수 없습니다.")
        String description
) {

}
