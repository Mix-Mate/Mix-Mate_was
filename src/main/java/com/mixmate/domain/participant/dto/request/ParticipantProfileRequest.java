package com.mixmate.domain.participant.dto.request;

import com.mixmate.domain.participant.entity.ParticipantProfile;
import com.mixmate.domain.participant.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ParticipantProfileRequest(

    @NotBlank(message = "보여질 이름을 입력하세요")
    @Size(max = 10, message = "보여질 이름은 10자를 넘을 수 없습니다.")
    @Pattern(regexp = "^[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9 ._-]*$",
            message = "보여질 이름에는 한글, 영문, 숫자와 일부 기호만 사용할 수 있습니다.")
    String displayName,

    @NotBlank(message = "학번을 입력해주세요.")
    @Size(max = 20, message = "학번은 20자를 넘을 수 없습니다.")
    @Pattern(regexp = "^[0-9]*$", message = "학번은 숫자만 사용할 수 있습니다.")
    String studentId,

    @NotNull(message = "직급을 선택해주세요.")
    Position position,

    @NotBlank(message = "전공을 입력해주세요.")
    @Size(max = 20, message = "전공은 20자를 넘을 수 없습니다.")
    @Pattern(regexp = "^[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9 ._-]*$",
            message = "전공에는 한글, 영문, 숫자와 일부 기호만 사용할 수 있습니다.")
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

    @Size(max = 30, message = "인스타 아이디는 30자를 넘을 수 없습니다.")
    @Pattern(regexp = "^[a-zA-Z0-9._]*$",
            message = "인스타 아이디는 영문, 숫자, 마침표, 밑줄만 사용할 수 있습니다.")
    String instaId,

    @Size(max = 120, message = "자기소개는 120자를 넘을 수 없습니다.")
    @Pattern(regexp = "^[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9 \\n\\r.,!?~()/&+#@:%*=;'_-]*$",
            message = "자기소개에는 한글, 영문, 숫자와 일부 기호만 사용할 수 있습니다.")
    String bio,

    @NotNull(message = "프로필 공개 여부를 선택해주세요.")
    Visibility visibility
    ){

    public ParticipantProfile toEntity() {
        return ParticipantProfile.builder()
                .displayName(displayName)
                .studentId(studentId)
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
