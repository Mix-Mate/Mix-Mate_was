package com.mixmate.domain.group.controller;

import com.mixmate.domain.group.dto.GroupCreateRequest;
import com.mixmate.domain.group.dto.GroupCreateResponse;
import com.mixmate.domain.group.dto.GroupUpdateRequest;
import com.mixmate.domain.group.service.GroupService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups")
@SecurityRequirement(name = "JWT")
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupCreateResponse> createGroup(
            @Valid @RequestBody GroupCreateRequest dto,
            @RequestHeader("X-User-Id") Long userId
            // TODO: auth 머지되면 @AuthenticationPrincipal CustomUserDetails userDetails로 교체
            ) {
        GroupCreateResponse response = groupService.createGroup(dto, userId);
        return ResponseEntity
                .created(URI.create("/api/v1/groups/" + response.groupId()))
                .body(response);
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<Void> updateGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupUpdateRequest dto,
            @RequestHeader("X-User-Id") Long userId
            // TODO: auth 머지되면 @AuthenticationPrincipal CustomUserDetails userDetails로 교체
            ) {
        groupService.updateGroup(dto, groupId, userId);
        return ResponseEntity.noContent().build();
    }
}
