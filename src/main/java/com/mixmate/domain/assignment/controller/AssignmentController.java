package com.mixmate.domain.assignment.controller;

import com.mixmate.domain.assignment.api.AssignmentApi;
import com.mixmate.domain.assignment.dto.request.TeamGenerateRequest;
import com.mixmate.domain.assignment.dto.response.MyTeamResponse;
import com.mixmate.domain.assignment.dto.response.TeamListResponse;
import com.mixmate.domain.assignment.service.AssignmentService;
import com.mixmate.domain.participant.enums.Round;
import com.mixmate.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups")
public class AssignmentController implements AssignmentApi {

    private final AssignmentService assignmentService;

    /**
     * 조건에 따라 참가자를 조에 배치합니다. 관리자만 가능하며 아직 확정은 아닙니다.
     * @param groupId 편성할 그룹 식별자
     * @param round 편성할 차수
     * @param dto 조 개수, 배치 조건, 고정 멤버
     * @param userDetails 로그인한 사용자의 인증 정보
     * @return 조별 참가자 목록과 200
     */
    public ResponseEntity<TeamListResponse> generate(
            @PathVariable Long groupId,
            @PathVariable Round round,
            @Valid @RequestBody TeamGenerateRequest dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(assignmentService.generate(dto, round, groupId, userDetails.getUserId()));
    }

    /**
     * 편성 결과를 확정해 해당 차수를 시작합니다. 관리자만 가능하며 되돌릴 수 없습니다.
     * @param groupId 확정할 그룹 식별자
     * @param round 확정할 차수
     * @param userDetails 로그인한 사용자의 인증 정보
     * @return 본문 없는 204
     */
    public ResponseEntity<Void> confirm(
            @PathVariable Long groupId,
            @PathVariable Round round,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        assignmentService.confirm(round, groupId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 참가자가 해당 차수에서 자신이 배정된 조와 조원을 조회합니다. 편성이 확정된 뒤에만 가능합니다.
     * @param groupId 조회할 그룹 식별자
     * @param round 조회할 차수
     * @param userDetails 로그인한 사용자의 인증 정보
     * @return 내 조 번호와 조원 목록, 200
     */
    /**
     * 해당 차수의 조 편성 전체를 조회합니다. 편성이 확정된 뒤에만 가능합니다.
     * @param groupId 조회할 그룹 식별자
     * @param round 조회할 차수
     * @param userDetails 로그인한 사용자의 인증 정보
     * @return 조 번호 순으로 정렬된 조 목록, 200
     */
    public ResponseEntity<TeamListResponse> getTeams(
            @PathVariable Long groupId,
            @PathVariable Round round,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(assignmentService.getTeams(round, groupId, userDetails.getUserId()));
    }

    public ResponseEntity<MyTeamResponse> getMyTeam(
            @PathVariable Long groupId,
            @PathVariable Round round,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(assignmentService.getMyTeam(round, groupId, userDetails.getUserId()));
    }
}
