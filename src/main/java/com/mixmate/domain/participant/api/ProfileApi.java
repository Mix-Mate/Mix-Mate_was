package com.mixmate.domain.participant.api;

import com.mixmate.domain.participant.dto.response.MyProfileResponse;
import com.mixmate.security.CustomUserDetails;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(value = "/api/v1/profiles", produces = MediaType.APPLICATION_JSON_VALUE)
public interface ProfileApi {

    @GetMapping("/recent")
    ResponseEntity<MyProfileResponse> getRecentProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
