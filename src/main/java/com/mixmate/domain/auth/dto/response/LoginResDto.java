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
public class LoginResDto {

    @Schema(example = "1")
    private final Long userId;

    @Schema(example = "hi@example.com")
    private final String email;

    @Schema(example = "hihello")
    private final String userName;

    @Schema(example = "618hdjfnvs3jr1f....")
    private final String accessToken;

    @Schema(example = "618hdjfnvs3jr1f....")
    private final String refreshToken;

    public static LoginResDto fromEntity(User user, String accessToken, String refreshToken) {
        return LoginResDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .userName(user.getUserName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
