package com.mixmate.domain.group.controller;

import com.mixmate.domain.group.api.GroupApi;
import com.mixmate.domain.group.dto.request.GroupCreateRequest;
import com.mixmate.domain.group.dto.response.GroupCreateResponse;
import com.mixmate.domain.group.dto.request.GroupUpdateRequest;
import com.mixmate.domain.group.service.GroupService;
import com.mixmate.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups")
public class GroupController implements GroupApi {

    private final GroupService groupService;

    /**
     * 새 그룹을 만들고 생성자를 관리자 겸 첫 참가자로 등록합니다.
     * @param dto 그룹명·설명과 생성자 본인의 프로필
     * @param userDetails 로그인한 사용자의 인증 정보
     * @return 생성된 그룹 정보와 201, Location 헤더에 그룹 경로
     */
    public ResponseEntity<GroupCreateResponse> createGroup(
            @Valid @RequestBody GroupCreateRequest dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        GroupCreateResponse response = groupService.createGroup(dto, userDetails.getUserId());
        return ResponseEntity
                .created(URI.create("/api/v1/groups/" + response.groupId()))
                .body(response);
    }

    /**
     * 그룹명과 설명을 수정합니다. 관리자만, 조 편성 전에만 가능합니다.
     * @param groupId 수정할 그룹 식별자
     * @param dto 새 그룹명과 설명
     * @param userDetails 로그인한 사용자의 인증 정보
     * @return 본문 없는 204
     */
    public ResponseEntity<Void> updateGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupUpdateRequest dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        groupService.updateGroup(dto, groupId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 그룹과 그 참가자를 함께 삭제합니다. 관리자만, 조 편성 전에만 가능합니다.
     * @param groupId 삭제할 그룹 식별자
     * @param userDetails 로그인한 사용자의 인증 정보
     * @return 본문 없는 204
     */
    public ResponseEntity<Void> deleteGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        groupService.deleteGroup(groupId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}
