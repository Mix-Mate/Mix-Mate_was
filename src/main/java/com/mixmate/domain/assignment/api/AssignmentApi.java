package com.mixmate.domain.assignment.api;

import com.mixmate.domain.assignment.dto.request.TeamGenerateRequest;
import com.mixmate.domain.assignment.dto.response.TeamAssignmentResponse;
import com.mixmate.domain.participant.enums.Round;
import com.mixmate.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping(value = "/api/v1/groups", produces = MediaType.APPLICATION_JSON_VALUE)
public interface AssignmentApi {

    @PostMapping("/{groupId}/rounds/{round}/teams/generate")
    ResponseEntity<TeamAssignmentResponse> generate(
            @PathVariable Long groupId,
            @PathVariable Round round,
            @Valid @RequestBody TeamGenerateRequest dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @PostMapping("/{groupId}/rounds/{round}/teams/confirm")
    ResponseEntity<Void> confirm(
            @PathVariable Long groupId,
            @PathVariable Round round,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
