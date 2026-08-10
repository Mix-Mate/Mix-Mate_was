package com.mixmate.domain.group.dto;

import com.mixmate.domain.participant.dto.ParticipantProfileRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GroupCreateRequest(

    @NotBlank(message = "그룹 이름을 입력해주세요.")
    String groupName,

    String description,

    @NotNull(message = "프로필 정보를 입력해주세요.")
    @Valid
    ParticipantProfileRequest profile
    ){

}
