package com.mixmate.domain.vote.dto.request;

import com.mixmate.domain.vote.enums.VoteChoice;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class Round2VoteReqDto {

    @Schema(description = "2차 참여 여부", example = "PARTICIPATE")
    @NotNull(message = "참여 여부를 선택해주세요.")
    private final VoteChoice choice;
}
