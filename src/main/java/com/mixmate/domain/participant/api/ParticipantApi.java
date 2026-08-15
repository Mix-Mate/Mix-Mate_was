package com.mixmate.domain.participant.api;

import com.mixmate.domain.participant.dto.request.ParticipantProfileRequest;
import com.mixmate.domain.participant.dto.response.MyProfileResponse;
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
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "참가자", description = "참가자 목록·프로필 조회, 참가자 추가·삭제, 그룹 탈퇴 API")
@RequestMapping(value = "/api/v1/groups", produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = "JWT")
public interface ParticipantApi {

    @Operation(summary = "참가자 목록 조회",
            description = "그룹의 참가자를 차수별로 조회합니다. "
                    + "카드 표시에 필요한 최소 정보만 내려주며, 상세 프로필은 별도 조회합니다. "
                    + "FIRST_ROUND는 조 편성이 끝난 뒤부터 조회할 수 있지만, "
                    + "관리자는 참가자를 추가·삭제해야 하므로 조 편성 전에도 조회할 수 있습니다. "
                    + "SECOND_ROUND는 2차 참석자만 포함되며, 2차 진행이 확정된 뒤부터 조회할 수 있습니다. "
                    + "조 편성 전이라도 참석자는 정해져 있으므로 조회됩니다.")
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
                                { "code": "FORBIDDEN", "message": "그룹에 대한 참가정보가 없습니다." }
                            """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 그룹",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "NOT_FOUND", "message": "그룹정보가 없습니다." }
                            """))),
            @ApiResponse(responseCode = "409", description = "참가자가 조 편성 전에 요청했거나, 2차 진행이 확정되기 전에 SECOND_ROUND를 요청",
                    content = @Content(examples = {
                            @ExampleObject(name = "아직 조 편성 전", value = """
                                        { "code": "INVALID_GROUP_STATUS", "message": "조 편성이 완료된 이후에 조회할 수 있습니다." }
                                    """),
                            @ExampleObject(name = "아직 2차 확정 전", value = """
                                        { "code": "INVALID_GROUP_STATUS", "message": "2차 진행 상태에서만 조회할 수 있습니다." }
                                    """)
                    }))
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
                    content = @Content(examples = {
                            @ExampleObject(name = "참가자가 아님", value = """
                                        { "code": "FORBIDDEN", "message": "그룹에 대한 참가정보가 없습니다." }
                                    """),
                            @ExampleObject(name = "비공개 프로필", value = """
                                        { "code": "FORBIDDEN", "message": "비공개 프로필입니다." }
                                    """)
                    })),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 그룹이거나 그 그룹에 없는 참가자",
                    content = @Content(examples = {
                            @ExampleObject(name = "그룹 없음", value = """
                                        { "code": "NOT_FOUND", "message": "그룹정보가 없습니다." }
                                    """),
                            @ExampleObject(name = "참가자 없음", value = """
                                        { "code": "NOT_FOUND", "message": "참가자를 찾을 수 없습니다." }
                                    """)
                    }))
    })
    @GetMapping("/{groupId}/participants/{participantId}")
    ResponseEntity<ParticipantProfileResponse> getParticipantProfile(
            @Parameter(description = "그룹 식별자", required = true) @PathVariable Long groupId,
            @Parameter(description = "조회할 참가자 식별자", required = true) @PathVariable Long participantId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "내 프로필 조회",
            description = "요청자 본인의 그룹 프로필을 조회합니다. 프로필 수정 화면에 기존 값을 채우는 용도이므로 "
                    + "응답이 수정 요청의 본문과 같은 모양이며, 다른 참가자 조회에는 없는 공개 여부(visibility)가 포함됩니다. "
                    + "그룹 진행 상태와 무관하게 호출할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MyProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 없음",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "UNAUTHORIZED", "message": "토큰이 없거나 만료되었습니다." }
                            """))),
            @ApiResponse(responseCode = "403", description = "이 그룹의 참가자가 아님",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "FORBIDDEN", "message": "그룹에 대한 참가정보가 없습니다." }
                            """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 그룹",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "NOT_FOUND", "message": "그룹정보가 없습니다." }
                            """)))
    })
    @GetMapping("/{groupId}/participants/me")
    ResponseEntity<MyProfileResponse> getMyProfile(
            @Parameter(description = "그룹 식별자", required = true) @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "내 프로필 수정",
            description = "요청자 본인의 그룹 프로필을 수정합니다. 참가자 모집 중에만 가능하며, "
                    + "모집을 마감하면 수정할 수 없습니다. "
                    + "PUT이므로 전체 교체이며, age·instaId·bio를 보내지 않으면 null로 지워집니다. "
                    + "수정 대상은 이 그룹의 프로필뿐이며, 다른 그룹의 프로필에는 영향이 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "프로필 수정 성공"),
            @ApiResponse(responseCode = "400", description = "필수값 누락, enum 값 불일치 등 입력값 오류",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 없음",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "UNAUTHORIZED", "message": "토큰이 없거나 만료되었습니다." }
                            """))),
            @ApiResponse(responseCode = "403", description = "이 그룹의 참가자가 아님",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "FORBIDDEN", "message": "그룹에 대한 참가정보가 없습니다." }
                            """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 그룹",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "NOT_FOUND", "message": "그룹정보가 없습니다." }
                            """))),
            @ApiResponse(responseCode = "409", description = "이미 조 편성이 끝난 그룹",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "INVALID_GROUP_STATUS", "message": "참가자 모집 중에만 프로필 수정이 가능합니다." }
                            """)))
    })
    @PutMapping("/{groupId}/participants/me")
    ResponseEntity<Void> updateParticipantProfile(
            @Parameter(description = "그룹 식별자", required = true) @PathVariable Long groupId,
            @Valid @RequestBody ParticipantProfileRequest dto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "그룹 탈퇴",
            description = "요청자 본인의 참가 정보를 삭제해 그룹에서 나갑니다. "
                    + "모집을 마감하면 명단에서 빠질 수 없으므로 모집 중에만 가능하며, "
                    + "관리자는 탈퇴할 수 없고 그룹 삭제만 가능합니다. "
                    + "다른 참가자를 내보내는 것은 관리자용 참가자 삭제 API를 사용합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "탈퇴 성공",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 없음",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "UNAUTHORIZED", "message": "토큰이 없거나 만료되었습니다." }
                            """))),
            @ApiResponse(responseCode = "403", description = "이 그룹의 참가자가 아니거나, 요청자가 관리자",
                    content = @Content(examples = {
                            @ExampleObject(name = "참가자가 아님", value = """
                                        { "code": "FORBIDDEN", "message": "그룹에 대한 참가정보가 없습니다." }
                                    """),
                            @ExampleObject(name = "관리자는 탈퇴 불가", value = """
                                        { "code": "FORBIDDEN", "message": "관리자는 탈퇴할 수 없습니다. 그룹을 삭제해 주세요." }
                                    """)
                    })),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 그룹",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "NOT_FOUND", "message": "그룹정보가 없습니다." }
                            """))),
            @ApiResponse(responseCode = "409", description = "이미 조 편성이 끝난 그룹",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "INVALID_GROUP_STATUS", "message": "참가자 모집 중에만 탈퇴할 수 있습니다." }
                            """)))
    })
    @DeleteMapping("/{groupId}/participants/me")
    ResponseEntity<Void> leaveGroup(
            @Parameter(description = "그룹 식별자", required = true) @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "참가자 삭제",
            description = "관리자가 다른 참가자를 그룹에서 내보냅니다. "
                    + "1차가 시작되면 명단이 고정되므로 그 전에만 가능하며, 모집 마감 이후에도 할 수 있습니다. "
                    + "관리자 본인은 대상으로 지정할 수 없습니다. 그룹을 정리하려면 그룹 삭제를 사용합니다. "
                    + "대리 등록된 오프라인 참가자도 같은 방식으로 삭제됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 없음",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "UNAUTHORIZED", "message": "토큰이 없거나 만료되었습니다." }
                            """))),
            @ApiResponse(responseCode = "403", description = "이 그룹의 참가자가 아니거나, 관리자가 아니거나, 관리자 본인을 지정",
                    content = @Content(examples = {
                            @ExampleObject(name = "참가자가 아님", value = """
                                        { "code": "FORBIDDEN", "message": "그룹에 대한 참가정보가 없습니다." }
                                    """),
                            @ExampleObject(name = "관리자가 아님", value = """
                                        { "code": "NOT_GROUP_ADMIN", "message": "관리자 권한이 필요합니다." }
                                    """),
                            @ExampleObject(name = "관리자 본인은 삭제 불가", value = """
                                        { "code": "FORBIDDEN", "message": "호스트 본인은 삭제할 수 없습니다." }
                                    """)
                    })),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 그룹이거나, 그 그룹에 없는 참가자",
                    content = @Content(examples = {
                            @ExampleObject(name = "그룹 없음", value = """
                                        { "code": "NOT_FOUND", "message": "그룹정보가 없습니다." }
                                    """),
                            @ExampleObject(name = "삭제 대상 없음", value = """
                                        { "code": "NOT_FOUND", "message": "삭제대상을 찾을 수 없습니다." }
                                    """)
                    })),
            @ApiResponse(responseCode = "409", description = "이미 조 편성이 끝난 그룹",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "INVALID_GROUP_STATUS", "message": "1차 진행 이전에만 참가자를 삭제할 수 있습니다." }
                            """)))
    })
    @DeleteMapping("/{groupId}/participants/{participantId}")
    ResponseEntity<Void> deleteParticipant(
            @Parameter(description = "그룹 식별자", required = true) @PathVariable Long groupId,
            @Parameter(description = "삭제할 참가자 식별자", required = true) @PathVariable Long participantId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "참가자 추가",
            description = "관리자가 로그인 계정이 없는 오프라인 참가자를 대신 등록합니다. "
                    + "1차가 시작되면 명단이 고정되므로 그 전에만 가능하며, 모집 마감 이후에도 할 수 있습니다. "
                    + "이렇게 추가된 참가자는 계정과 연결되지 않아 본인이 프로필을 수정할 수 없으므로, "
                    + "잘못 입력했다면 삭제 후 다시 추가합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "참가자 추가 성공. 응답 헤더 Location에 추가된 참가자 경로가 담깁니다.",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "필수값 누락, enum 값 불일치 등 입력값 오류",
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
            @ApiResponse(responseCode = "409", description = "이미 조 편성이 끝난 그룹",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "INVALID_GROUP_STATUS", "message": "1차 진행 이전에만 참가자를 추가할 수 있습니다." }
                            """)))
    })
    @PostMapping("/{groupId}/participants")
    ResponseEntity<Void> addParticipant(
            @Parameter(description = "그룹 식별자", required = true) @PathVariable Long groupId,
            @Valid @RequestBody ParticipantProfileRequest dto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
