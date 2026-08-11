package com.mixmate.domain.participant.api;

import com.mixmate.domain.participant.dto.response.ParticipantListResponse;
import com.mixmate.domain.participant.dto.response.ParticipantProfileResponse;
import com.mixmate.domain.participant.enums.Round;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "참가자", description = "참가자 목록, 프로필 조회 API")
@RequestMapping(value = "/api/v1/groups", produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = "JWT")
public interface ParticipantApi {

    @Operation(summary = "참가자 목록 조회",
            description = "조 편성이 끝난 그룹의 참가자를 차수별로 조회합니다. "
                    + "카드 표시에 필요한 최소 정보만 내려주며, 상세 프로필은 별도 조회합니다. "
                    + "SECOND_ROUND는 2차 참석자만 포함됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ParticipantListResponse.class))),
            @ApiResponse(responseCode = "400", description = "round 값이 enum에 없음",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 없음",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "UNAUTHORIZED", "message": "토큰이 없거나 만료되었습니다." }
                            """))),
            @ApiResponse(responseCode = "403", description = "이 그룹의 참가자가 아님",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "FORBIDDEN", "message": "권한이 없습니다." }
                            """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 그룹",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "NOT_FOUND", "message": "대상을 찾을 수 없습니다." }
                            """))),
            @ApiResponse(responseCode = "409", description = "아직 조 편성 전",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "INVALID_GROUP_STATUS", "message": "현재 진행 상태에서 불가능한 요청입니다." }
                            """)))
    })
    @GetMapping("/{groupId}/participants")
    ResponseEntity<ParticipantListResponse> getParticipants(
            @Parameter(description = "그룹 식별자", required = true) @PathVariable Long groupId,
            @Parameter(description = "조회할 차수", required = true) @RequestParam Round round,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "참가자 프로필 조회",
            description = "같은 그룹 참가자의 상세 프로필을 조회합니다. "
                    + "대상이 비공개(PRIVATE)면 거부되지만, 본인 프로필은 공개 여부와 무관하게 항상 조회됩니다. "
                    + "그룹 진행 상태와 무관하게 호출할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ParticipantProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 없음",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "UNAUTHORIZED", "message": "토큰이 없거나 만료되었습니다." }
                            """))),
            @ApiResponse(responseCode = "403", description = "이 그룹의 참가자가 아니거나, 대상이 비공개 프로필",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "FORBIDDEN", "message": "권한이 없습니다." }
                            """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 그룹이거나 그 그룹에 없는 참가자",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "NOT_FOUND", "message": "대상을 찾을 수 없습니다." }
                            """)))
    })
    @GetMapping("/{groupId}/participants/{participantId}")
    ResponseEntity<ParticipantProfileResponse> getParticipantProfile(
            @Parameter(description = "그룹 식별자", required = true) @PathVariable Long groupId,
            @Parameter(description = "조회할 참가자 식별자", required = true) @PathVariable Long participantId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
