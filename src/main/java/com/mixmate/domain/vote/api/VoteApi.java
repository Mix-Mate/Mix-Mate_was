package com.mixmate.domain.vote.api;

import com.mixmate.domain.vote.dto.request.AdminRound2VoteReqDto;
import com.mixmate.domain.vote.dto.request.MvpVoteReqDto;
import com.mixmate.domain.vote.dto.request.Round2VoteReqDto;
import com.mixmate.domain.vote.dto.response.Round2VoteStatusResDto;
import com.mixmate.domain.vote.dto.response.VoteResultResDto;
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

@Tag(name = "투표", description = "MVP 투표, 2차 참여 여부 투표와 그 현황·결과 조회, 관리자의 투표 종료 API")
@RequestMapping(value = "/api/v1/groups", produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = "JWT")
public interface VoteApi {

    @Operation(summary = "MVP 투표하기",
            description = "1차 술자리에서 가장 분위기를 잘 이끈 같은 조원 한 명에게 투표합니다. "
                    + "자신에게는 투표할 수 없고, 1차 조 편성 기준으로 같은 조원에게만 투표할 수 있으며, "
                    + "투표는 한 번만 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "투표 성공"),
            @ApiResponse(responseCode = "400", description = "필수값 누락 등 입력값 오류",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 없음",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "UNAUTHORIZED", "message": "토큰이 없거나 만료되었습니다." }
                            """))),
            @ApiResponse(responseCode = "403", description = "이 그룹의 참가자가 아님",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "FORBIDDEN", "message": "그룹에 대한 참가정보가 없습니다." }
                            """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 그룹이거나 투표 대상을 찾을 수 없음",
                    content = @Content(examples = {
                            @ExampleObject(name = "그룹 없음", value = """
                                        { "code": "NOT_FOUND", "message": "그룹정보가 없습니다." }
                                    """),
                            @ExampleObject(name = "대상 없음", value = """
                                        { "code": "NOT_FOUND", "message": "투표 대상을 찾을 수 없습니다." }
                                    """)
                    })),
            @ApiResponse(responseCode = "409", description = "투표 미진행, 이미 투표함, 본인/타 조원에게 투표, 조 편성 미완료",
                    content = @Content(examples = {
                            @ExampleObject(name = "투표 미진행", value = """
                                        { "code": "VOTE_NOT_IN_PROGRESS", "message": "투표가 진행중이 아닙니다." }
                                    """),
                            @ExampleObject(name = "중복 투표", value = """
                                        { "code": "ALREADY_VOTED", "message": "이미 투표를 완료했습니다." }
                                    """),
                            @ExampleObject(name = "본인에게 투표", value = """
                                        { "code": "VOTE_TARGET_NOT_IN_TEAM", "message": "자신에게는 투표할 수 없습니다." }
                                    """),
                            @ExampleObject(name = "다른 조원에게 투표", value = """
                                        { "code": "VOTE_TARGET_NOT_IN_TEAM", "message": "같은 조원에게만 투표할 수 있습니다." }
                                    """),
                            @ExampleObject(name = "조 편성 미완료", value = """
                                        { "code": "INVALID_GROUP_STATUS", "message": "조 편성이 완료되지 않았습니다." }
                                    """)
                    }))
    })
    @PostMapping("/{groupId}/votes/mvp")
    ResponseEntity<Void> voteMvp(
            @Parameter(description = "그룹 식별자", required = true) @PathVariable Long groupId,
            @Valid @RequestBody MvpVoteReqDto dto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "2차 참여 여부 투표하기",
            description = "2차 술자리 참여 여부를 투표합니다. 참여를 선택하면 참가자의 2차 참여 상태가 함께 갱신됩니다. "
                    + "투표는 한 번만 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "투표 성공"),
            @ApiResponse(responseCode = "400", description = "필수값 누락 등 입력값 오류",
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
            @ApiResponse(responseCode = "409", description = "투표 미진행 또는 이미 투표함",
                    content = @Content(examples = {
                            @ExampleObject(name = "투표 미진행", value = """
                                        { "code": "VOTE_NOT_IN_PROGRESS", "message": "투표가 진행중이 아닙니다." }
                                    """),
                            @ExampleObject(name = "중복 투표", value = """
                                        { "code": "ALREADY_VOTED", "message": "이미 투표를 완료했습니다." }
                                    """)
                    }))
    })
    @PostMapping("/{groupId}/votes/second-round")
    ResponseEntity<Void> voteSecondRound(
            @Parameter(description = "그룹 식별자", required = true) @PathVariable Long groupId,
            @Valid @RequestBody Round2VoteReqDto dto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "관리자가 대신 2차 참여 여부 투표하기",
            description = "로그인 계정이 없는(관리자가 대리 등록한) 참가자를 대신해 관리자가 2차 참여 여부를 투표합니다. "
                    + "계정이 있는 참가자는 본인만 투표할 수 있으므로 이 API로 대신 투표할 수 없습니다. "
                    + "참여를 선택하면 참가자의 2차 참여 상태가 함께 갱신됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "투표 성공"),
            @ApiResponse(responseCode = "400", description = "필수값 누락 등 입력값 오류",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 없음",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "UNAUTHORIZED", "message": "토큰이 없거나 만료되었습니다." }
                            """))),
            @ApiResponse(responseCode = "403", description = "이 그룹의 참가자가 아니거나 관리자가 아님, 또는 계정이 있는 참가자를 대상으로 함",
                    content = @Content(examples = {
                            @ExampleObject(name = "참가자가 아님", value = """
                                        { "code": "FORBIDDEN", "message": "그룹에 대한 참가정보가 없습니다." }
                                    """),
                            @ExampleObject(name = "관리자가 아님", value = """
                                        { "code": "NOT_GROUP_ADMIN", "message": "관리자 권한이 필요합니다." }
                                    """),
                            @ExampleObject(name = "계정이 있는 참가자", value = """
                                        { "code": "NOT_MANUAL_PARTICIPANT", "message": "로그인 계정이 있는 참가자는 관리자가 대신 투표할 수 없습니다." }
                                    """)
                    })),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 그룹이거나 투표 대상을 찾을 수 없음",
                    content = @Content(examples = {
                            @ExampleObject(name = "그룹 없음", value = """
                                        { "code": "NOT_FOUND", "message": "그룹정보가 없습니다." }
                                    """),
                            @ExampleObject(name = "대상 없음", value = """
                                        { "code": "NOT_FOUND", "message": "투표 대상을 찾을 수 없습니다." }
                                    """)
                    })),
            @ApiResponse(responseCode = "409", description = "투표 미진행 또는 이미 투표함",
                    content = @Content(examples = {
                            @ExampleObject(name = "투표 미진행", value = """
                                        { "code": "VOTE_NOT_IN_PROGRESS", "message": "투표가 진행중이 아닙니다." }
                                    """),
                            @ExampleObject(name = "중복 투표", value = """
                                        { "code": "ALREADY_VOTED", "message": "이미 투표를 완료했습니다." }
                                    """)
                    }))
    })
    @PostMapping("/{groupId}/votes/second-round/admin")
    ResponseEntity<Void> voteSecondRoundByHost(
            @Parameter(description = "그룹 식별자", required = true) @PathVariable Long groupId,
            @Valid @RequestBody AdminRound2VoteReqDto dto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "2차 투표 현황 확인하기",
            description = "그룹 참가자 전체의 2차 참여 여부 투표 현황(완료/참여/불참/미투표 인원과 참가자별 상태)을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Round2VoteStatusResDto.class))),
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
    @GetMapping("/{groupId}/votes/second-round/status")
    ResponseEntity<Round2VoteStatusResDto> getSecondRoundVoteStatus(
            @Parameter(description = "그룹 식별자", required = true) @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "투표 결과 확인하기",
            description = "오늘의 MVP(동점자가 있으면 공동 MVP로 모두 포함)와 2차 참여자 명단을 조회합니다. "
                    + "투표가 종료된 이후에만 조회할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = VoteResultResDto.class))),
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
            @ApiResponse(responseCode = "409", description = "아직 투표가 종료되지 않음",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "INVALID_GROUP_STATUS", "message": "투표가 아직 종료되지 않았습니다." }
                            """)))
    })
    @GetMapping("/{groupId}/votes/result")
    ResponseEntity<VoteResultResDto> getVoteResult(
            @Parameter(description = "그룹 식별자", required = true) @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "전체 투표 종료하기",
            description = "관리자가 투표를 강제 종료합니다. 아직 투표하지 않은 참가자는 모두 2차 불참으로 자동 처리되며, "
                    + "그룹은 조 편성 대기 상태로 넘어갑니다. 되돌릴 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "종료 성공"),
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
            @ApiResponse(responseCode = "409", description = "투표가 진행중이 아님",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "VOTE_NOT_IN_PROGRESS", "message": "투표가 진행중이 아닙니다." }
                            """)))
    })
    @PostMapping("/{groupId}/votes/finish")
    ResponseEntity<Void> finishVote(
            @Parameter(description = "그룹 식별자", required = true) @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
