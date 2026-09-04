package com.mixmate.domain.vote.controller;

import com.mixmate.domain.vote.api.VoteApi;
import com.mixmate.domain.vote.dto.request.AdminRound2VoteReqDto;
import com.mixmate.domain.vote.dto.request.MvpVoteReqDto;
import com.mixmate.domain.vote.dto.request.Round2VoteReqDto;
import com.mixmate.domain.vote.dto.response.Round2VoteStatusResDto;
import com.mixmate.domain.vote.dto.response.VoteResultResDto;
import com.mixmate.domain.vote.service.VoteService;
import com.mixmate.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups")
public class VoteController implements VoteApi {

    private final VoteService voteService;

    public ResponseEntity<Void> voteMvp(
            @PathVariable Long groupId,
            @Valid @RequestBody MvpVoteReqDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        voteService.voteMvp(dto, groupId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<Void> voteSecondRound(
            @PathVariable Long groupId,
            @Valid @RequestBody Round2VoteReqDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        voteService.voteSecondRound(dto, groupId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<Void> updateSecondRoundVote(
            @PathVariable Long groupId,
            @Valid @RequestBody Round2VoteReqDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        voteService.updateSecondRoundVote(dto, groupId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<Void> voteSecondRoundByHost(
            @PathVariable Long groupId,
            @Valid @RequestBody AdminRound2VoteReqDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        voteService.voteSecondRoundByHost(dto, groupId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<Void> updateSecondRoundVoteByHost(
            @PathVariable Long groupId,
            @Valid @RequestBody AdminRound2VoteReqDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        voteService.updateSecondRoundVoteByHost(dto, groupId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<Round2VoteStatusResDto> getSecondRoundVoteStatus(
            @PathVariable Long groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(voteService.getSecondRoundVoteStatus(groupId, userDetails.getUserId()));
    }

    public ResponseEntity<VoteResultResDto> getVoteResult(
            @PathVariable Long groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(voteService.getVoteResult(groupId, userDetails.getUserId()));
    }

    public ResponseEntity<Void> finishVote(
            @PathVariable Long groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        voteService.finishVote(groupId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}
