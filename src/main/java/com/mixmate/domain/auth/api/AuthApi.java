package com.mixmate.domain.auth.api;

import com.mixmate.domain.auth.dto.request.LoginReqDto;
import com.mixmate.domain.auth.dto.request.PasswordResetReqDto;
import com.mixmate.domain.auth.dto.request.SignupReqDto;
import com.mixmate.domain.auth.dto.request.TokenReissueReqDto;
import com.mixmate.domain.auth.dto.request.WithdrawReqDto;
import com.mixmate.domain.auth.dto.response.LoginResDto;
import com.mixmate.domain.auth.dto.response.TokenReissueResDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "인증", description = "회원가입, 이메일 인증, 로그인, 로그아웃 API. "
        + "이 태그의 대부분 API는 로그인 없이 호출 가능하지만, 회원탈퇴(withdraw)는 로그인이 필요합니다.")
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public interface AuthApi {

    @Operation(summary = "회원가입",
            description = "이메일 인증(verify)이 완료된 상태에서만 가입할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "필수값 누락 등 입력값 오류, 또는 이메일 인증 미완료",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "EMAIL_NOT_VERIFIED", "message": "이메일 인증이 완료되지 않았습니다." }
                            """))),
            @ApiResponse(responseCode = "409", description = "이미 가입된 이메일",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "EMAIL_CONFLICTED", "message": "이미 가입된 이메일입니다." }
                            """)))
    })
    @PostMapping("/signup")
    ResponseEntity<String> signup(@Valid @RequestBody SignupReqDto signupReqDto);

    @Operation(summary = "인증번호 발송", description = "회원가입할 이메일로 6자리 인증번호를 발송합니다. 5분간 유효합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발송 성공")
    })
    @PostMapping("/send")
    ResponseEntity<String> sendCode(
            @Parameter(description = "인증번호를 받을 이메일", required = true) @RequestParam String email
    );

    @Operation(summary = "인증번호 검증", description = "발송된 인증번호가 맞는지 확인합니다. 성공 시 10분간 회원가입이 가능한 상태가 됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증 성공"),
            @ApiResponse(responseCode = "400", description = "인증번호가 틀렸거나 만료됨")
    })
    @PostMapping("/verify")
    ResponseEntity<String> verifyCode(
            @Parameter(description = "이메일", required = true) @RequestParam String email,
            @Parameter(description = "발송받은 6자리 인증번호", required = true) @RequestParam String code
    );

    @Operation(summary = "비밀번호 재설정 인증번호 발송",
            description = "가입된 이메일로 6자리 인증번호를 발송합니다. 5분간 유효합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발송 성공"),
            @ApiResponse(responseCode = "404", description = "가입되지 않은 이메일",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "USER_NOT_FOUND", "message": "사용자를 찾을 수 없습니다." }
                            """)))
    })
    @PostMapping("/password/send")
    ResponseEntity<String> sendPasswordResetCode(
            @Parameter(description = "인증번호를 받을, 가입된 이메일", required = true) @RequestParam String email
    );

    @Operation(summary = "비밀번호 재설정 인증번호 검증",
            description = "발송된 인증번호가 맞는지 확인합니다. 성공 시 10분간 비밀번호 재설정이 가능한 상태가 됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증 성공"),
            @ApiResponse(responseCode = "400", description = "인증번호가 틀렸거나 만료됨")
    })
    @PostMapping("/password/verify")
    ResponseEntity<String> verifyPasswordResetCode(
            @Parameter(description = "이메일", required = true) @RequestParam String email,
            @Parameter(description = "발송받은 6자리 인증번호", required = true) @RequestParam String code
    );

    @Operation(summary = "비밀번호 재설정",
            description = "인증번호 검증(password/verify)이 완료된 이메일만 비밀번호를 재설정할 수 있습니다. "
                    + "성공 시 다른 기기의 로그인을 무효화하기 위해 리프레시 토큰을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재설정 성공"),
            @ApiResponse(responseCode = "400", description = "필수값 누락 등 입력값 오류, 또는 인증 미완료",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "EMAIL_NOT_VERIFIED", "message": "이메일 인증이 완료되지 않았습니다." }
                            """))),
            @ApiResponse(responseCode = "404", description = "가입되지 않은 이메일",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "USER_NOT_FOUND", "message": "사용자를 찾을 수 없습니다." }
                            """)))
    })
    @PostMapping("/password/reset")
    ResponseEntity<String> resetPassword(@Valid @RequestBody PasswordResetReqDto passwordResetReqDto);

    @Operation(summary = "로그인",
            description = "이메일/비밀번호로 로그인합니다. accessToken/refreshToken을 응답 본문과 httpOnly 쿠키로 함께 내려줍니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoginResDto.class))),
            @ApiResponse(responseCode = "400", description = "이메일 또는 비밀번호 누락, 이메일 형식 오류",
                    content = @Content(examples = @ExampleObject(value = """
                                {
                                  "code": "INVALID_PARAMETER",
                                  "message": "입력값이 올바르지 않습니다.",
                                  "errors": { "email": "올바른 이메일 형식이 아닙니다." }
                                }
                            """))),
            @ApiResponse(responseCode = "401", description = "비밀번호 불일치",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "INVALID_PASSWORD", "message": "이메일 또는 비밀번호가 일치하지 않습니다." }
                            """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "USER_NOT_FOUND", "message": "사용자를 찾을 수 없습니다." }
                            """)))
    })
    @PostMapping("/login")
    ResponseEntity<LoginResDto> login(@Valid @RequestBody LoginReqDto loginReqDto);

    @Operation(summary = "accessToken 재발급",
            description = "refreshToken으로 새 accessToken을 발급합니다. refreshToken은 쿠키를 우선 사용하고, "
                    + "쿠키가 없으면 요청 바디의 refreshToken을 봅니다. 성공 시 새 accessToken을 쿠키로도 다시 심어줍니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TokenReissueResDto.class))),
            @ApiResponse(responseCode = "401", description = "refreshToken이 없거나, 유효하지 않거나, 로그인 때 저장된 값과 다름",
                    content = @Content(examples = {
                            @ExampleObject(name = "토큰 없음", value = """
                                        { "code": "UNAUTHORIZED", "message": "리프레시 토큰이 없습니다." }
                                    """),
                            @ExampleObject(name = "서명 오류·만료", value = """
                                        { "code": "JWT_TOKEN_PARSING_ERROR", "message": "토큰이 유효하지 않거나 만료되었습니다." }
                                    """),
                            @ExampleObject(name = "저장된 값과 불일치(로그아웃됨 등)", value = """
                                        { "code": "UNAUTHORIZED", "message": "리프레시 토큰이 유효하지 않습니다." }
                                    """)
                    })),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "USER_NOT_FOUND", "message": "사용자를 찾을 수 없습니다." }
                            """)))
    })
    @PostMapping("/reissue")
    ResponseEntity<TokenReissueResDto> reissue(
            @RequestBody(required = false) TokenReissueReqDto reissueReqDto,
            @Parameter(hidden = true) HttpServletRequest request
    );

    @Operation(summary = "로그아웃",
            description = "accessToken 쿠키를 읽어 refreshToken을 삭제하고 accessToken을 블랙리스트에 등록한 뒤, 클라이언트 쿠키를 만료시킵니다. "
                    + "쿠키가 없거나 이미 만료된 토큰이어도 에러 없이 성공 처리됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    @PostMapping("/logout")
    ResponseEntity<String> logout(@Parameter(hidden = true) HttpServletRequest request);

    @Operation(summary = "회원 탈퇴 (로그인 필요)",
            description = "본인 확인을 위해 현재 비밀번호를 함께 받습니다. 계정 행은 지우지 않고 탈퇴 시각만 기록하는 "
                    + "소프트 딜리트 방식이며, 지금까지 참여한 그룹·투표 기록은 그대로 남습니다. "
                    + "성공 시 리프레시 토큰을 삭제하고 현재 액세스 토큰을 블랙리스트에 등록해 모든 기기의 로그인을 즉시 무효화합니다. "
                    + "탈퇴한 계정으로는 다시 로그인할 수 없지만, 같은 이메일로 새로 회원가입하는 것은 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "탈퇴 성공"),
            @ApiResponse(responseCode = "401", description = "로그인이 필요하거나(토큰 없음/만료) 비밀번호가 일치하지 않음",
                    content = @Content(examples = {
                            @ExampleObject(name = "로그인 필요", value = """
                                        { "code": "UNAUTHORIZED", "message": "로그인이 필요합니다." }
                                    """),
                            @ExampleObject(name = "비밀번호 불일치", value = """
                                        { "code": "INVALID_PASSWORD", "message": "이메일 또는 비밀번호가 일치하지 않습니다." }
                                    """)
                    })),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "USER_NOT_FOUND", "message": "사용자를 찾을 수 없습니다." }
                            """)))
    })
    @DeleteMapping("/withdraw")
    ResponseEntity<String> withdraw(@Valid @RequestBody WithdrawReqDto withdrawReqDto,
                                     @Parameter(hidden = true) HttpServletRequest request);
}
