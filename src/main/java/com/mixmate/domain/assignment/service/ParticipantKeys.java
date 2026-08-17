package com.mixmate.domain.assignment.service;

import com.mixmate.domain.assignment.enums.AssignmentCondition;
import com.mixmate.domain.participant.entity.Participant;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 조건별로 "이 값이 같으면 같은 부류"를 정하는 기준입니다.
 *
 * 편성(TeamAssigner)과 요약(AssignmentSummarizer)이 같은 기준으로 부류를 세야 하므로 한곳에 모아 둡니다.
 * 한쪽만 바꾸면 "조건을 지켰다고 계산해 놓고 요약에서는 못 지켰다고 나오는" 상태가 됩니다.
 */
final class ParticipantKeys {

    private static final Map<AssignmentCondition, Function<Participant, Object>> KEYS =
            new EnumMap<>(AssignmentCondition.class);

    static {
        KEYS.put(AssignmentCondition.GENDER_BALANCE, p -> p.getProfile().getGender());
        KEYS.put(AssignmentCondition.MBTI_BALANCE, p -> p.getProfile().getMbti().isExtrovert());
        KEYS.put(AssignmentCondition.GRADE_SPREAD, p -> p.getProfile().getGrade());
        KEYS.put(AssignmentCondition.MAJOR_SPREAD, p -> p.getProfile().getMajor());
        KEYS.put(AssignmentCondition.NEWCOMER_SPREAD, p -> p.getProfile().isNew());
        KEYS.put(AssignmentCondition.POSITION_SPREAD, p -> p.getProfile().getPosition());
    }

    /** 상수를 추가하고 여기에 넣지 않으면 조용히 무시되는 대신 바로 터진다. */
    static Function<Participant, Object> of(AssignmentCondition condition) {
        return Objects.requireNonNull(KEYS.get(condition), () -> condition + "의 분류 기준이 없습니다.");
    }

    private ParticipantKeys() {
    }
}
