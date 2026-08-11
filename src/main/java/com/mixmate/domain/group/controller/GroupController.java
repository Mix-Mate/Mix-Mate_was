package com.mixmate.domain.group.controller;

import com.mixmate.domain.group.dto.request.GroupCreateRequest;
import com.mixmate.domain.group.dto.response.GroupCreateResponse;
import com.mixmate.domain.group.dto.request.GroupUpdateRequest;
import com.mixmate.domain.group.service.GroupService;
import com.mixmate.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        GroupCreateResponse response = groupService.createGroup(dto, userDetails.getUserId());
        return ResponseEntity
                .created(URI.create("/api/v1/groups/" + response.groupId()))
                .body(response);
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<Void> updateGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupUpdateRequest dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        groupService.updateGroup(dto, groupId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        groupService.deleteGroup(groupId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}
