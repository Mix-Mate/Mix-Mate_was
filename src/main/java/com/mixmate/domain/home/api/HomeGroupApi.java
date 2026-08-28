package com.mixmate.domain.home.api;

import com.mixmate.domain.home.dto.request.HomeGroupJoinReqDto;
import com.mixmate.domain.home.dto.request.HomeInviteCodeVerifyReqDto;
import com.mixmate.domain.home.dto.response.HomeGroupListResDto;
import com.mixmate.domain.home.dto.response.HomeInviteCodeVerifyResDto;
import com.mixmate.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "홈", description = "메인 홈 화면의 그룹 참여코드 검증, 입장 API")
@RequestMapping(value = "/api/v1/groups", produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = "JWT")
public interface HomeGroupApi {

    @Operation(summary = "참여코드 검증",
            description = "입력한 참여코드가 실제로 존재하는 그룹인지 확인합니다. "
                    + "프로필 입력 화면으로 넘어가기 전에 먼저 코드만 빠르게 검증할 때 사용합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검증 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = HomeInviteCodeVerifyResDto.class))),
            @ApiResponse(responseCode = "400", description = "참여코드 누락",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 없음",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "UNAUTHORIZED", "message": "토큰이 없거나 만료되었습니다." }
                            """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 참여코드",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "INVALID_INVITE_CODE", "message": "유효하지 않은 초대코드입니다." }
                            """)))
    })
    @PostMapping("/invitations/verify")
    ResponseEntity<HomeInviteCodeVerifyResDto> verifyInviteCode(
            @Valid @RequestBody HomeInviteCodeVerifyReqDto dto
    );

    @Operation(summary = "그룹 입장",
            description = "참여코드와 프로필 정보를 함께 보내 그룹에 참가자로 입장합니다. "
                    + "조 편성이 이미 시작된 그룹이거나 이미 참여중인 그룹이면 실패합니다. "
                    + "관리자에게 삭제된 사용자는 차단되므로, 해제되기 전까지는 같은 참여코드로도 입장할 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "입장 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = HomeInviteCodeVerifyResDto.class))),
            @ApiResponse(responseCode = "400", description = "필수값 누락, enum 값 불일치 등 입력값 오류",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 없음",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "UNAUTHORIZED", "message": "토큰이 없거나 만료되었습니다." }
                            """))),
            @ApiResponse(responseCode = "403", description = "관리자에게 삭제되어 차단된 사용자",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "BANNED_FROM_GROUP", "message": "차단되어 입장할 수 없습니다." }
                            """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 참여코드",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "INVALID_INVITE_CODE", "message": "유효하지 않은 초대코드입니다." }
                            """))),
            @ApiResponse(responseCode = "409", description = "이미 참여중이거나, 조 편성이 시작되어 더 이상 입장 불가",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "ALREADY_JOINED", "message": "이미 참여중인 그룹입니다." }
                            """)))
    })
    @PostMapping("/invitations/join")
    ResponseEntity<HomeInviteCodeVerifyResDto> joinGroup(
            @Valid @RequestBody HomeGroupJoinReqDto dto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "내 그룹 목록 조회",
            description = "로그인한 사용자 본인이 참여중인 그룹 목록을 조회합니다(관리자·일반 참여자 모두 포함). "
                    + "state=active면 FINISHED를 제외한 진행중인 그룹만, state=finished면 종료된 그룹만 내려줍니다. "
                    + "참여 그룹이 없으면 빈 배열로 200을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = HomeGroupListResDto.class))),
            @ApiResponse(responseCode = "400", description = "scope 또는 state 값이 올바르지 않음",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "INVALID_PARAMETER", "message": "입력값이 올바르지 않습니다." }
                            """))),
            @ApiResponse(responseCode = "401", description = "인증 없음",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "UNAUTHORIZED", "message": "토큰이 없거나 만료되었습니다." }
                            """)))
    })
    @GetMapping
    ResponseEntity<HomeGroupListResDto> getMyGroups(
            @Parameter(description = "조회 범위. 현재는 \"me\"만 지원", required = true) @RequestParam String scope,
            @Parameter(description = "\"active\"(진행중) 또는 \"finished\"(종료)", required = true) @RequestParam String state,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
