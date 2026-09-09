package com.mixmate.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNameUpdateReqDto {

    @Schema(description = "마이페이지에 표시할 이름. 최대 10자", example = "홍길동")
    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 10, message = "이름은 최대 10자까지 입력할 수 있습니다.")
    @Pattern(regexp = "^[^<>]*$", message = "이름에 <, > 문자는 사용할 수 없습니다.")
    private final String userName;
}
