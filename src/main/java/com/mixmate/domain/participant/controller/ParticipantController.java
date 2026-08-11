package com.mixmate.domain.participant.controller;

import com.mixmate.domain.participant.api.ParticipantApi;
import com.mixmate.domain.participant.dto.response.ParticipantListResponse;
import com.mixmate.domain.participant.dto.response.ParticipantProfileResponse;
import com.mixmate.domain.participant.enums.Round;
import com.mixmate.domain.participant.service.ParticipantService;
import com.mixmate.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups")
public class ParticipantController implements ParticipantApi {

    private final ParticipantService participantService;

    /**
     * 그룹의 참가자를 차수별로 조회합니다. 조 편성이 끝난 뒤에만 가능합니다.
     * @param groupId 조회할 그룹 식별자
     * @param round 조회할 차수
     * @param userDetails 로그인한 사용자의 인증 정보
     * @return 카드 표시용 최소 정보만 담은 참가자 목록
     */
    public ResponseEntity<ParticipantListResponse> getParticipants(
            @PathVariable Long groupId,
            @RequestParam Round round,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(participantService.getParticipants(groupId, round, userDetails.getUserId()));
    }

    /**
     * 같은 그룹 참가자의 상세 프로필을 조회합니다.
     * @param groupId 조회할 그룹 식별자
     * @param participantId 조회 대상 참가자 식별자
     * @param userDetails 로그인한 사용자의 인증 정보
     * @return 대상의 상세 프로필
     */
    public ResponseEntity<ParticipantProfileResponse> getParticipantProfile(
            @PathVariable Long groupId,
            @PathVariable Long participantId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(participantService.getParticipantProfile(groupId, participantId, userDetails.getUserId()));
    }
}
