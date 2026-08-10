package com.mixmate.domain.participant.dto;

import com.mixmate.domain.participant.entity.ParticipantProfile;
import com.mixmate.domain.participant.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ParticipantProfileRequest(

    @NotBlank(message = "보여질 이름을 입력하세요")
    String displayName,

    @NotNull(message = "직급을 선택해주세요.")
    Position position,

    @NotBlank(message = "전공을 입력해주세요.")
    String major,

    @NotNull(message = "신입 여부를 체크해주세요.")
    Boolean isNew,

    @NotNull(message = "학년을 선택해주세요.")
    Grade grade,

    @NotNull(message = "성별을 선택해주세요.")
    Gender gender,

    @NotNull(message = "MBTI를 선택해주세요.")
    Mbti mbti,

    Integer age,

    String instaId,

    String bio,

    @NotNull(message = "프로필 공개 여부를 선택해주세요.")
    Visibility visibility
    ){

    public ParticipantProfile toEntity() {
        return ParticipantProfile.builder()
                .displayName(displayName)
                .position(position)
                .major(major)
                .isNew(isNew)
                .grade(grade)
                .gender(gender)
                .mbti(mbti)
                .age(age)
                .instaId(instaId)
                .bio(bio)
                .visibility(visibility)
                .build();
    }
}
