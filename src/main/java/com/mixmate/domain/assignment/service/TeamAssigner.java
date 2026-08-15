package com.mixmate.domain.assignment.service;

import com.mixmate.domain.assignment.enums.AssignmentCondition;
import com.mixmate.domain.participant.entity.Participant;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

@Component
public class TeamAssigner {

    // 조건별로 "이 값이 같으면 같은 부류"를 정하는 기준.
    // SIZE_BALANCE는 부류를 세는 조건이 아니라 동점 처리 규칙이라 여기 없다.
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

    public Map<Integer, List<Participant>> assign(List<Participant> participants,
                                                  int teamCount,
                                                  Set<AssignmentCondition> conditions,
                                                  Map<Long, Integer> fixedMembers) {

        // 팀 초기화
        Map<Integer, List<Participant>> teams = new LinkedHashMap<>(teamCount);
        for (int teamNumber = 1; teamNumber <= teamCount; teamNumber++) {
            teams.put(teamNumber, new ArrayList<>());
        }

        // 배치 순서가 결과를 좌우하므로 섞는다. 같은 참가자 구성이라도 실행할 때마다 다른 편성이 나온다.
        List<Participant> shuffled = new ArrayList<>(participants);
        Collections.shuffle(shuffled);

        // 고정 멤버 추가
        for (Participant participant : shuffled) {
            Integer fixedTeam = fixedMembers.get(participant.getParticipantId());
            if (fixedTeam != null) {
                teams.get(fixedTeam).add(participant);
            }
        }

        // 고정 멤버 제외 추가
        for (Participant participant : shuffled) {
            if (fixedMembers.containsKey(participant.getParticipantId())) continue;
            teams.get(bestTeam(teams, participant, conditions)).add(participant);
        }
        return teams;
    }

    // 벌점이 가장 낮은 조를 고른다. 동점이면 인원 수 균등이 켜져 있을 때만 인원이 적은 조.
    private int bestTeam(Map<Integer, List<Participant>> teams,
                         Participant candidate,
                         Set<AssignmentCondition> conditions) {

        // 조 순회 순서도 섞는다. 조건을 전부 끄면 모든 조의 벌점이 0이라
        // 항상 1번부터 훑으면 전원이 1조로 몰린다.
        List<Integer> order = new ArrayList<>(teams.keySet());
        Collections.shuffle(order);

        boolean sizeBalance = conditions.contains(AssignmentCondition.SIZE_BALANCE);
        int bestTeam = -1;
        int bestPenalty = Integer.MAX_VALUE;
        int bestSize = Integer.MAX_VALUE;

        for (int teamNumber : order) {
            List<Participant> team = teams.get(teamNumber);
            int penalty = penaltyOf(team, candidate, conditions);
            int size = team.size();

            if (penalty < bestPenalty || (penalty == bestPenalty && sizeBalance && size < bestSize)) {
                bestTeam = teamNumber;
                bestPenalty = penalty;
                bestSize = size;
            }
        }
        return bestTeam;
    }

    // 이 참가자를 이 조에 넣었을 때의 벌점.
    // 켜진 조건마다 "그 조에 이미 있는 같은 부류의 수"를 세서 더한다. 낮을수록 고르게 섞인 조다.
    private int penaltyOf(List<Participant> team,
                          Participant candidate,
                          Set<AssignmentCondition> conditions) {

        int penalty = 0;
        for (AssignmentCondition condition : conditions) {
            Function<Participant, Object> key = KEYS.get(condition);
            if (key == null) continue;   // SIZE_BALANCE

            Object candidateKey = key.apply(candidate);
            for (Participant member : team) {
                // major는 String, MBTI 키는 Boolean이라 ==로 비교하면 조용히 틀린다
                if (Objects.equals(key.apply(member), candidateKey)) penalty++;
            }
        }
        return penalty;
    }
}
