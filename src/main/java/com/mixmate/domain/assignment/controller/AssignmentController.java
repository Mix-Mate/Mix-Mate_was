package com.mixmate.domain.assignment.controller;

import com.mixmate.domain.assignment.api.AssignmentApi;
import com.mixmate.domain.assignment.dto.request.TeamGenerateRequest;
import com.mixmate.domain.assignment.dto.response.TeamAssignmentResponse;
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

    public ResponseEntity<TeamAssignmentResponse> generate(
            @PathVariable Long groupId,
            @PathVariable Round round,
            @Valid @RequestBody TeamGenerateRequest dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(assignmentService.generate(dto, round, groupId, userDetails.getUserId()));
    }

    public ResponseEntity<Void> confirm(
            @PathVariable Long groupId,
            @PathVariable Round round,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        assignmentService.confirm(round, groupId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}
