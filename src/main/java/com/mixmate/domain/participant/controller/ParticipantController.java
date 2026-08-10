package com.mixmate.domain.participant.controller;

import com.mixmate.domain.participant.dto.ParticipantListResponse;
import com.mixmate.domain.participant.dto.ParticipantProfileResponse;
import com.mixmate.domain.participant.enums.Round;
import com.mixmate.domain.participant.service.ParticipantService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups")
@SecurityRequirement(name = "JWT")
public class ParticipantController {

    private final ParticipantService participantService;

    @GetMapping("/{groupId}/participants")
    public ResponseEntity<ParticipantListResponse> getParticipants(
            @PathVariable Long groupId,
            @RequestParam Round round,
            @RequestHeader("X-User-Id") Long userId
            // TODO: auth 머지되면 @AuthenticationPrincipal CustomUserDetails userDetails로 교체
    ) {
        return ResponseEntity.ok(participantService.getParticipants(groupId, round, userId));
    }

    @GetMapping("/{groupId}/participants/{participantId}")
    public ResponseEntity<ParticipantProfileResponse> getParticipantProfile(
            @PathVariable Long groupId,
            @PathVariable Long participantId,
            @RequestHeader("X-User-Id") Long userId
            // TODO: auth 머지되면 @AuthenticationPrincipal CustomUserDetails userDetails로 교체
    ) {
        return ResponseEntity.ok(participantService.getParticipantProfile(groupId, participantId, userId));
    }
}
