package com.mixmate.domain.home.controller;

import com.mixmate.domain.home.api.HomeGroupApi;
import com.mixmate.domain.home.dto.request.HomeGroupJoinReqDto;
import com.mixmate.domain.home.dto.request.HomeInviteCodeVerifyReqDto;
import com.mixmate.domain.home.dto.response.HomeInviteCodeVerifyResDto;
import com.mixmate.domain.home.service.HomeGroupService;
import com.mixmate.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups")
public class HomeGroupController implements HomeGroupApi {

    private final HomeGroupService homeGroupService;

    /**
     * 참여코드가 실제로 존재하는 그룹인지 검증합니다.
     * @param dto 참여코드
     * @return 검증된 그룹의 최소 정보
     */
    @PostMapping("/invitations/verify")
    public ResponseEntity<HomeInviteCodeVerifyResDto> verifyInviteCode(
            @Valid @RequestBody HomeInviteCodeVerifyReqDto dto
    ) {
        return ResponseEntity.ok(homeGroupService.verifyInviteCode(dto));
    }

    /**
     * 참여코드와 프로필을 함께 보내 그룹에 참가자로 입장합니다.
     * @param dto 참여코드와 본인 프로필
     * @param userDetails 로그인한 사용자의 인증 정보
     * @return 입장한 그룹의 최소 정보
     */
    @PostMapping("/invitations/join")
    public ResponseEntity<HomeInviteCodeVerifyResDto> joinGroup(
            @Valid @RequestBody HomeGroupJoinReqDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(homeGroupService.joinGroup(dto, userDetails.getUserId()));
    }
}
