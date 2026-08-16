package com.mixmate.domain.assignment.api;

import com.mixmate.domain.assignment.dto.request.TeamGenerateRequest;
import com.mixmate.domain.assignment.dto.response.MyTeamResponse;
import com.mixmate.domain.assignment.dto.response.TeamAssignmentResponse;
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

@Tag(name = "조 편성", description = "라운드별 조 편성 실행·확정과 배정 결과 조회 API")
@RequestMapping(value = "/api/v1/groups", produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = "JWT")
public interface AssignmentApi {

    @Operation(summary = "조 편성 실행",
            description = """
                    관리자가 배치 조건과 조 개수를 정해 조를 편성합니다. 결과는 저장되지만 아직 확정은 아니며,
                    확정 API를 호출해야 라운드가 시작됩니다.

                    조건(conditions)은 켜진 것만 배열로 보냅니다. 전부 끄면 빈 배열이고 조건 없이 무작위로 나눕니다.
                    조건과 무관하게 조별 인원은 항상 고르게 나뉘므로, 인원 균등은 조건으로 보내지 않습니다.
                    고정 멤버(fixedMembers)는 지정한 조에 먼저 배치되고 나머지가 조건에 따라 흩어집니다. 없으면 빈 배열입니다.
                    고정 멤버를 한 조에 몰아넣지 않는 한 조별 인원 차이는 1명 이하입니다.

                    1차는 그룹 참가자 전원이, 2차는 2차 참여를 선택한 인원만 대상입니다.

                    확정 전이라면 같은 요청을 다시 보낼 수 있습니다(재셔플).
                    그때마다 이전 편성 결과를 덮어쓰며, 조 개수와 조건도 새로 보낸 값으로 바뀝니다.
                    확정한 뒤에는 다시 편성할 수 없습니다.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "편성 성공. 조별 참가자 목록을 돌려줍니다. 재실행이어도 같은 형식입니다.",
                    content = @Content(schema = @Schema(implementation = TeamAssignmentResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "round": "FIRST_ROUND",
                                      "teamCount": 2,
                                      "conditions": ["GENDER_BALANCE", "MAJOR_SPREAD"],
                                      "teams": [
                                        {
                                          "teamNumber": 1,
                                          "members": [
                                            { "participantId": 304, "displayName": "김대현", "major": "컴퓨터공학과", "visibility": "PUBLIC", "fixed": true },
                                            { "participantId": 308, "displayName": "이서연", "major": "컴퓨터공학과", "visibility": "PRIVATE", "fixed": false }
                                          ]
                                        },
                                        {
                                          "teamNumber": 2,
                                          "members": [
                                            { "participantId": 305, "displayName": "박지호", "major": "전기공학과", "visibility": "PUBLIC", "fixed": false }
                                          ]
                                        }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = """
                    고정 멤버 지정이 잘못됐거나(공통 형식), 요청 값 검증에 실패함(공통 형식 아님).
                    입력값 검증 실패와 경로의 round 오타는 code 필드가 없는 스프링 기본 응답으로 나갑니다.""",
                    content = @Content(examples = {
                            @ExampleObject(name = "고정 멤버 오류", value = """
                                        { "code": "INVALID_PARAMETER", "message": "조 편성 대상이 아닌 참가자는 고정할 수 없습니다." }
                                    """),
                            @ExampleObject(name = "조 번호 범위 밖", value = """
                                        { "code": "INVALID_PARAMETER", "message": "조 번호가 조 개수를 벗어났습니다." }
                                    """),
                            @ExampleObject(name = "중복 고정", value = """
                                        { "code": "INVALID_PARAMETER", "message": "같은 참가자를 두 번 고정할 수 없습니다." }
                                    """),
                            @ExampleObject(name = "입력값 검증 실패 (code 없음)", value = """
                                        {
                                          "timestamp": "2026-08-16T06:42:06.595+00:00",
                                          "status": 400,
                                          "error": "Bad Request",
                                          "path": "/api/v1/groups/1/rounds/FIRST_ROUND/teams/generate"
                                        }
                                    """)
                    })),
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
            @ApiResponse(responseCode = "409", description = "편성할 수 있는 상태가 아니거나(이미 확정한 경우 포함), 조당 2명을 채우지 못함",
                    content = @Content(examples = {
                            @ExampleObject(name = "편성 대기 상태 아님", value = """
                                        { "code": "INVALID_GROUP_STATUS", "message": "조 편성 대기 중일 때만 편성할 수 있습니다." }
                                    """),
                            @ExampleObject(name = "인원 부족", value = """
                                        { "code": "INSUFFICIENT_PARTICIPANTS", "message": "조 편성에 필요한 최소 인원을 충족하지 못했습니다." }
                                    """)
                    }))
    })
    @PostMapping("/{groupId}/rounds/{round}/teams/generate")
    ResponseEntity<TeamAssignmentResponse> generate(
            @Parameter(description = "그룹 식별자", required = true) @PathVariable Long groupId,
            @Parameter(description = "편성할 차수", required = true) @PathVariable Round round,
            @Valid @RequestBody TeamGenerateRequest dto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "조 편성 확정",
            description = """
                    관리자가 편성 결과를 확정해 해당 라운드를 시작합니다.
                    그룹 상태가 FIRST_ROUND 또는 SECOND_ROUND로 바뀌며 참가자가 자기 조를 볼 수 있게 됩니다. 되돌릴 수 없습니다.

                    편성을 실행하지 않고 호출하면 409입니다.""")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "확정 성공. 이미 그 라운드가 시작된 그룹에 다시 호출해도 204입니다."),
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
            @ApiResponse(responseCode = "409", description = "확정할 수 있는 상태가 아니거나, 편성을 실행하지 않음",
                    content = @Content(examples = {
                            @ExampleObject(name = "편성 대기 상태 아님", value = """
                                        { "code": "INVALID_GROUP_STATUS", "message": "조 편성 대기 중일 때만 확정할 수 있습니다." }
                                    """),
                            @ExampleObject(name = "편성 안 함", value = """
                                        { "code": "INVALID_GROUP_STATUS", "message": "조 편성을 먼저 실행해야 합니다." }
                                    """)
                    }))
    })
    @PostMapping("/{groupId}/rounds/{round}/teams/confirm")
    ResponseEntity<Void> confirm(
            @Parameter(description = "그룹 식별자", required = true) @PathVariable Long groupId,
            @Parameter(description = "확정할 차수", required = true) @PathVariable Round round,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "조 편성 전체 조회",
            description = """
                    해당 차수의 조 편성 전체를 조회합니다. 참가자 목록 화면의 '조별 보기'에 쓰입니다.

                    조 편성이 확정된 뒤에만 볼 수 있습니다. 편성 실행 응답과 같은 형식입니다.
                    2차 참여를 선택하지 않은 참가자는 2차 편성을 볼 수 없습니다. 관리자는 참여하지 않더라도 조회할 수 있습니다.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공. 조 번호 순으로 정렬된 조 목록을 돌려줍니다.",
                    content = @Content(schema = @Schema(implementation = TeamAssignmentResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "round": "FIRST_ROUND",
                                      "teamCount": 2,
                                      "conditions": ["GENDER_BALANCE", "MAJOR_SPREAD"],
                                      "teams": [
                                        {
                                          "teamNumber": 1,
                                          "members": [
                                            { "participantId": 435, "displayName": "이서연", "major": "컴퓨터공학과", "visibility": "PUBLIC", "fixed": false },
                                            { "participantId": 436, "displayName": "박지호", "major": "전기공학과", "visibility": "PRIVATE", "fixed": false }
                                          ]
                                        },
                                        {
                                          "teamNumber": 2,
                                          "members": [
                                            { "participantId": 434, "displayName": "김대현", "major": "컴퓨터공학과", "visibility": "PUBLIC", "fixed": true }
                                          ]
                                        }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "경로의 round 값이 enum에 없음. code 필드가 없는 스프링 기본 응답입니다.",
                    content = @Content(examples = @ExampleObject(value = """
                                {
                                  "timestamp": "2026-08-16T10:04:40.839+00:00",
                                  "status": 400,
                                  "error": "Bad Request",
                                  "path": "/api/v1/groups/1/rounds/THIRD_ROUND/teams"
                                }
                            """))),
            @ApiResponse(responseCode = "401", description = "인증 없음",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "UNAUTHORIZED", "message": "토큰이 없거나 만료되었습니다." }
                            """))),
            @ApiResponse(responseCode = "403", description = "이 그룹의 참가자가 아니거나, 해당 차수의 편성 대상이 아님",
                    content = @Content(examples = {
                            @ExampleObject(name = "참가자가 아님", value = """
                                        { "code": "FORBIDDEN", "message": "그룹에 대한 참가정보가 없습니다." }
                                    """),
                            @ExampleObject(name = "2차 불참자", value = """
                                        { "code": "FORBIDDEN", "message": "2차 참여를 선택한 참가자만 조회할 수 있습니다." }
                                    """)
                    })),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 그룹",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "NOT_FOUND", "message": "그룹정보가 없습니다." }
                            """))),
            @ApiResponse(responseCode = "409", description = "조 편성이 아직 확정되지 않았거나, 해당 차수의 편성이 없음",
                    content = @Content(examples = {
                            @ExampleObject(name = "확정 전", value = """
                                        { "code": "INVALID_GROUP_STATUS", "message": "조 편성이 확정된 뒤에 조회할 수 있습니다." }
                                    """),
                            @ExampleObject(name = "편성 없음", value = """
                                        { "code": "INVALID_GROUP_STATUS", "message": "해당 차수의 조 편성이 없습니다." }
                                    """)
                    }))
    })
    @GetMapping("/{groupId}/rounds/{round}/teams")
    ResponseEntity<TeamAssignmentResponse> getTeams(
            @Parameter(description = "그룹 식별자", required = true) @PathVariable Long groupId,
            @Parameter(description = "조회할 차수", required = true) @PathVariable Round round,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "내 조 조회",
            description = """
                    참가자가 해당 차수에서 자신이 배정된 조와 조원을 조회합니다. 관리자도 참가자로서 조회합니다.

                    조 편성이 확정된 뒤에만 볼 수 있습니다. 확정 전 결과는 관리자가 다시 편성하는 중일 수 있어 열지 않습니다.
                    2차 참여를 선택하지 않은 참가자는 2차 배치에 포함되지 않아 조회할 수 없습니다.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공. 내 조 번호와 조원 목록을 돌려줍니다.",
                    content = @Content(schema = @Schema(implementation = MyTeamResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "round": "FIRST_ROUND",
                                      "team": {
                                        "teamNumber": 2,
                                        "members": [
                                          { "participantId": 304, "displayName": "김대현", "major": "컴퓨터공학과", "visibility": "PUBLIC", "fixed": false },
                                          { "participantId": 309, "displayName": "최수아", "major": "산업디자인", "visibility": "PUBLIC", "fixed": true }
                                        ]
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "경로의 round 값이 enum에 없음. code 필드가 없는 스프링 기본 응답입니다.",
                    content = @Content(examples = @ExampleObject(value = """
                                {
                                  "timestamp": "2026-08-16T06:42:06.595+00:00",
                                  "status": 400,
                                  "error": "Bad Request",
                                  "path": "/api/v1/groups/1/rounds/FIRST_ROUND/teams/my-team"
                                }
                            """))),
            @ApiResponse(responseCode = "401", description = "인증 없음",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "UNAUTHORIZED", "message": "토큰이 없거나 만료되었습니다." }
                            """))),
            @ApiResponse(responseCode = "403", description = "이 그룹의 참가자가 아니거나, 해당 차수의 편성 대상이 아님",
                    content = @Content(examples = {
                            @ExampleObject(name = "참가자가 아님", value = """
                                        { "code": "FORBIDDEN", "message": "그룹에 대한 참가정보가 없습니다." }
                                    """),
                            @ExampleObject(name = "2차 불참자", value = """
                                        { "code": "FORBIDDEN", "message": "2차 참여를 선택한 참가자만 조회할 수 있습니다." }
                                    """)
                    })),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 그룹",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "NOT_FOUND", "message": "그룹정보가 없습니다." }
                            """))),
            @ApiResponse(responseCode = "409", description = "조 편성이 아직 확정되지 않았거나, 해당 차수의 편성이 없음",
                    content = @Content(examples = {
                            @ExampleObject(name = "확정 전", value = """
                                        { "code": "INVALID_GROUP_STATUS", "message": "조 편성이 확정된 뒤에 조회할 수 있습니다." }
                                    """),
                            @ExampleObject(name = "편성 없음", value = """
                                        { "code": "INVALID_GROUP_STATUS", "message": "해당 차수의 조 편성이 없습니다." }
                                    """)
                    }))
    })
    @GetMapping("/{groupId}/rounds/{round}/teams/my-team")
    ResponseEntity<MyTeamResponse> getMyTeam(
            @Parameter(description = "그룹 식별자", required = true) @PathVariable Long groupId,
            @Parameter(description = "조회할 차수", required = true) @PathVariable Round round,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
