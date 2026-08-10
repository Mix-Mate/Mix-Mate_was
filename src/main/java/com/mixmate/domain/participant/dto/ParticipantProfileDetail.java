package com.mixmate.domain.participant.dto;

import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.entity.ParticipantProfile;
import com.mixmate.domain.participant.enums.Gender;
import com.mixmate.domain.participant.enums.Grade;
import com.mixmate.domain.participant.enums.Mbti;
import com.mixmate.domain.participant.enums.Position;
import lombok.Builder;

@Builder
public record ParticipantProfileDetail(
        String displayName,
        Grade grade,
        Gender gender,
        String major,
        Boolean isNew,
        Position position,
        Mbti mbti,
        Integer age,
        String instaId,
        String bio
) {
    public static ParticipantProfileDetail from(Participant participant) {
        ParticipantProfile profile = participant.getProfile();
        return ParticipantProfileDetail.builder()
                .displayName(profile.getDisplayName())
                .grade(profile.getGrade())
                .gender(profile.getGender())
                .major(profile.getMajor())
                .isNew(profile.isNew()).
                position(profile.getPosition())
                .mbti(profile.getMbti())
                .age(profile.getAge())
                .instaId(profile.getInstaId())
                .bio(profile.getBio())
                .build();
    }
}
