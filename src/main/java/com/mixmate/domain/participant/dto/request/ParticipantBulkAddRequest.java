package com.mixmate.domain.participant.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ParticipantBulkAddRequest(
        @NotEmpty(message = "추가할 참가자를 한 명 이상 입력해주세요.")
        @Size(max = 100, message = "한 번에 최대로 추가할 수 있는 인원은 100명입니다.")
        @Valid
        List<ParticipantProfileRequest> participants
) {
}
