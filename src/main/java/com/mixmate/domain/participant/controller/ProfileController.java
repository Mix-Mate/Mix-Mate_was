package com.mixmate.domain.participant.controller;

import com.mixmate.domain.participant.api.ProfileApi;
import com.mixmate.domain.participant.dto.response.MyProfileResponse;
import com.mixmate.domain.participant.service.ParticipantService;
import com.mixmate.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/profiles")
public class ProfileController implements ProfileApi {

    private final ParticipantService participantService;

    /**
     * 요청자가 가장 최근에 입력·수정한 프로필을 조회합니다. 그룹 생성·입장 폼의 prefill에 쓰입니다.
     * @param userDetails 로그인한 사용자의 인증 정보
     * @return 최근 프로필과 200, 참가 이력이 없으면 204
     */
    public ResponseEntity<MyProfileResponse> getRecentProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return participantService.getRecentProfile(userDetails.getUserId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
