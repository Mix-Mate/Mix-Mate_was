package com.mixmate.domain.group.dto.request;

import com.mixmate.domain.participant.dto.request.ParticipantProfileRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GroupCreateRequest(

    @NotBlank(message = "그룹 이름을 입력해주세요.")
    @Size(max = 30, message = "그룹 이름은 30자를 넘을 수 없습니다.")
    @Pattern(regexp = "^[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9 .,!?~()/&+#@:_-]*$",
            message = "그룹 이름에는 한글, 영문, 숫자와 일부 기호만 사용할 수 있습니다.")
    String groupName,

    @Size(max = 120, message = "그룹 설명은 120자를 넘을 수 없습니다.")
    @Pattern(regexp = "^[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9 \\n\\r.,!?~()/&+#@:%*=;'_-]*$",
            message = "그룹 설명에는 한글, 영문, 숫자와 일부 기호만 사용할 수 있습니다.")
    String description,

    @NotNull(message = "프로필 정보를 입력해주세요.")
    @Valid
    ParticipantProfileRequest profile
    ){

}
