package com.mixmate.domain.auth.dto.response;

import com.mixmate.domain.auth.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class MyInfoResDto {

    @Schema(example = "1")
    private final Long userId;

    @Schema(example = "hi@example.com")
    private final String email;

    @Schema(example = "hihello")
    private final String userName;

    @Schema(description = "가입 경로", example = "KAKAO")
    private final String provider;

    @Schema(description = "지금까지 참여한 모든 모임을 통틀어 MVP로 뽑힌 횟수", example = "3")
    private final long mvpCount;

    public static MyInfoResDto fromEntity(User user, long mvpCount) {
        return MyInfoResDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .userName(user.getUserName())
                .provider(user.getProvider().name())
                .mvpCount(mvpCount)
                .build();
    }
}
