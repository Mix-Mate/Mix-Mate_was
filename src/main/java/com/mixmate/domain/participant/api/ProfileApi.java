package com.mixmate.domain.participant.api;

import com.mixmate.domain.participant.dto.response.MyProfileResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "프로필", description = "특정 그룹에 속하지 않는 프로필 조회 API")
@RequestMapping(value = "/api/v1/profiles", produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = "JWT")
public interface ProfileApi {

    @Operation(summary = "최근 프로필 조회",
            description = "요청자가 가장 최근에 입력하거나 수정한 프로필을 조회합니다. "
                    + "그룹 생성·입장 화면의 프로필 입력 폼을 미리 채우는 용도입니다. "
                    + "프로필을 입력·수정한 시각이 기준이므로, 나중에 만든 그룹보다 먼저 만든 그룹의 "
                    + "프로필이 내려올 수 있습니다. "
                    + "그룹의 진행 상태는 가리지 않으며 이미 종료된 그룹의 프로필도 대상입니다. "
                    + "참여한 그룹이 한 번도 없으면 본문 없이 204를 반환합니다. "
                    + "응답은 내 프로필 조회와 같은 필드 구성이며, 이 API는 아무것도 저장하지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MyProfileResponse.class))),
            @ApiResponse(responseCode = "204", description = "참여한 그룹이 없어 내려줄 프로필이 없음",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 없음",
                    content = @Content(examples = @ExampleObject(value = """
                                { "code": "UNAUTHORIZED", "message": "토큰이 없거나 만료되었습니다." }
                            """)))
    })
    @GetMapping("/recent")
    ResponseEntity<MyProfileResponse> getRecentProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
