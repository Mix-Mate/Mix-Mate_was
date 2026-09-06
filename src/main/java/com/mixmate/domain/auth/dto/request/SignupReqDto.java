package com.mixmate.domain.auth.dto.request;

import com.mixmate.domain.auth.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class SignupReqDto {

    @Schema(description = "학교 이메일. 인증(verify)까지 완료된 이메일만 가입 가능", example = "hi@example.com")
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private final String email;

    @Schema(description = "비밀번호. 최소 8자", example = "password123")
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    private final String password;

    @Schema(description = "사용자 이름", example = "홍길동")
    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 10, message = "이름은 10자를 넘을 수 없습니다.")
    @Pattern(regexp = "^[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9 ._-]*$",
            message = "이름에는 한글, 영문, 숫자와 일부 기호만 사용할 수 있습니다.")
    private final String userName;

    public User toEntity(PasswordEncoder passwordEncoder) {
        return User.builder()
                .email(this.email)
                .password(passwordEncoder.encode(this.password))
                .userName(this.userName)
                .build();
    }
}
