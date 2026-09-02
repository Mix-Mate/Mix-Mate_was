package com.mixmate.domain.group.dto.request;

import com.mixmate.domain.participant.dto.request.ParticipantProfileRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record GroupCreateRequest(

    @NotBlank(message = "그룹 이름을 입력해주세요.")
    @Pattern(regexp = "^[^<>]*$", message = "그룹 이름에 <, > 문자는 사용할 수 없습니다.")
    String groupName,

    @Pattern(regexp = "^[^<>]*$", message = "설명에 <, > 문자는 사용할 수 없습니다.")
    String description,

    @NotNull(message = "프로필 정보를 입력해주세요.")
    @Valid
    ParticipantProfileRequest profile
    ){

}
