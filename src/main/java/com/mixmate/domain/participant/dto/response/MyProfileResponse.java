package com.mixmate.domain.participant.dto.response;

import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.entity.ParticipantProfile;
import com.mixmate.domain.participant.enums.*;

public record MyProfileResponse(
        String displayName,
        Position position,
        String major,
        Boolean isNew,
        Grade grade,
        Gender gender,
        Mbti mbti,
        Integer age,
        String instaId,
        String bio,
        Visibility visibility
) {
    public static MyProfileResponse from(Participant me) {
        ParticipantProfile profile = me.getProfile();
        return new MyProfileResponse(
                profile.getDisplayName(),
                profile.getPosition(),
                profile.getMajor(),
                profile.isNew(),
                profile.getGrade(),
                profile.getGender(),
                profile.getMbti(),
                profile.getAge(),
                profile.getInstaId(),
                profile.getBio(),
                profile.getVisibility()
        );
    }
}
