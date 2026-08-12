package com.mixmate.domain.group.api;

import com.mixmate.domain.group.dto.request.GroupCreateRequest;
import com.mixmate.domain.group.dto.request.GroupUpdateRequest;
import com.mixmate.domain.group.dto.response.GroupCreateResponse;
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
import org.springframework.web.bind.annotation.*;

@Tag(name = "그룹", description = "그룹 생성, 수정, 삭제 API")
@RequestMapping(value = "/api/v1/groups", produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = "JWT")
public interface GroupApi {

    @Operation(summary = "그룹 생성", description = "사용자가 그룹을 생성합니다. 그룹을 생성한 사용자는 그 그룹의 관리자가 됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "그룹 생성 성공. 응답 헤더 Location에 생성된 그룹 경로가 담깁니다.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GroupCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "필수값 누락, enum 값 불일치 등 입력값 오류",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 없음",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "UNAUTHORIZED", "message": "토큰이 없거나 만료되었습니다." }
                            """))),
            @ApiResponse(responseCode = "500", description = "초대코드 생성 반복 실패",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "INTERNAL_SERVER_ERROR", "message": "서버 오류가 발생하였습니다." }
                            """)))
    })
    @PostMapping
    ResponseEntity<GroupCreateResponse> createGroup(
            @Valid @RequestBody GroupCreateRequest dto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "그룹 정보 수정",
            description = "관리자가 조 편성 전에 그룹명과 설명을 수정합니다. "
                    + "PUT이므로 전체 교체이며, description을 보내지 않으면 null로 지워집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "그룹 수정 성공"),
            @ApiResponse(responseCode = "400", description = "그룹명 누락 등 입력값 오류",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 없음",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "UNAUTHORIZED", "message": "토큰이 없거나 만료되었습니다." }
                            """))),
            @ApiResponse(responseCode = "403", description = "이 그룹의 참가자가 아니거나, 관리자가 아님",
                    content = @Content(examples = {
                            @ExampleObject(name = "참가자가 아님", value = """
                                        { "code": "FORBIDDEN", "message": "그룹에 대한 참가정보가 없습니다." }
                                    """),
                            @ExampleObject(name = "관리자가 아님", value = """
                                        { "code": "NOT_GROUP_ADMIN", "message": "관리자 권한이 필요합니다." }
                                    """)
                    })),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 그룹",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "NOT_FOUND", "message": "그룹정보가 없습니다." }
                            """))),
            @ApiResponse(responseCode = "409", description = "조 편성 이후",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "INVALID_GROUP_STATUS", "message": "현재 진행 상태에서 불가능한 요청입니다." }
                            """)))
    })
    @PutMapping("/{groupId}")
    ResponseEntity<Void> updateGroup(
            @Parameter(description = "그룹 식별자", required = true) @PathVariable Long groupId,
            @Valid @RequestBody GroupUpdateRequest dto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "그룹 삭제",
            description = "관리자가 조 편성 전 그룹을 삭제합니다. 참가자 정보도 함께 삭제되며 되돌릴 수 없습니다. "
                    + "참가자의 계정 자체는 삭제되지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "그룹 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 없음",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "UNAUTHORIZED", "message": "토큰이 없거나 만료되었습니다." }
                            """))),
            @ApiResponse(responseCode = "403", description = "이 그룹의 참가자가 아니거나, 관리자가 아님",
                    content = @Content(examples = {
                            @ExampleObject(name = "참가자가 아님", value = """
                                        { "code": "FORBIDDEN", "message": "그룹에 대한 참가정보가 없습니다." }
                                    """),
                            @ExampleObject(name = "관리자가 아님", value = """
                                        { "code": "NOT_GROUP_ADMIN", "message": "관리자 권한이 필요합니다." }
                                    """)
                    })),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 그룹",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "NOT_FOUND", "message": "그룹정보가 없습니다." }
                            """))),
            @ApiResponse(responseCode = "409", description = "조 편성 이후",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "INVALID_GROUP_STATUS", "message": "현재 진행 상태에서 불가능한 요청입니다." }
                            """)))
    })
    @DeleteMapping("/{groupId}")
    ResponseEntity<Void> deleteGroup(
            @Parameter(description = "그룹 식별자", required = true) @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
