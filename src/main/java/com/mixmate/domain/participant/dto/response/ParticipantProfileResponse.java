package com.mixmate.domain.participant.dto.response;

import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.entity.ParticipantProfile;
import com.mixmate.domain.participant.enums.Gender;
import com.mixmate.domain.participant.enums.Grade;
import com.mixmate.domain.participant.enums.Mbti;
import com.mixmate.domain.participant.enums.Position;
import lombok.Builder;

@Builder
public record ParticipantProfileResponse(
        String displayName,
        Grade grade,
        Gender gender,
        String major,
        Boolean isNew,
        Position position,
        Mbti mbti,
        Integer age,
        String instaId,
        String bio,
        long mvpCount
) {
    /**
     * @param mvpCount 이 참가자 계정이 지금까지 참여한 모든 모임을 통틀어 MVP로 뽑힌 누적 횟수.
     *                 로그인 계정이 없는(관리자 대리 등록) 참가자는 집계할 계정 자체가 없어 항상 0이다.
     */
    public static ParticipantProfileResponse from(Participant participant, long mvpCount) {
        ParticipantProfile profile = participant.getProfile();
        return ParticipantProfileResponse.builder()
                .displayName(profile.getDisplayName())
                .grade(profile.getGrade())
                .gender(profile.getGender())
                .major(profile.getMajor())
                .isNew(profile.isNew())
                .position(profile.getPosition())
                .mbti(profile.getMbti())
                .age(profile.getAge())
                .instaId(profile.getInstaId())
                .bio(profile.getBio())
                .mvpCount(mvpCount)
                .build();
    }
}
