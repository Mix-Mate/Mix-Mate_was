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

/**
 * 참가자를 조에 나누는 계산만 담당합니다. 저장도 검증도 하지 않아 스프링 없이 단위 테스트할 수 있습니다.
 *
 * 방식은 그리디입니다. 참가자를 한 명씩 꺼내 "이 조에 넣으면 얼마나 나빠지는지"를 벌점으로 재고
 * 가장 낮은 조에 넣습니다. 벌점은 켜진 조건마다 그 조에 이미 있는 같은 부류의 수를 더한 값입니다.
 * 벌점이 같으면 인원이 적은 조로 보내므로 조건과 무관하게 인원은 항상 고르게 나뉩니다.
 *
 * 한 번의 그리디는 참가자를 꺼내는 순서에 좌우되어 운이 나쁘면 나쁜 편성이 그대로 나옵니다.
 * 그래서 여러 번 돌린 뒤 가장 나은 하나를 고릅니다.
 *
 * 완전한 최적해(NP-hard)가 아니라 근사값이며, 참가자 순서를 섞기 때문에 같은 입력이라도 실행할 때마다 결과가 달라집니다.
 */
@Component
public class TeamAssigner {

    /** 그리디를 돌리는 횟수. 참가자가 백 명 안쪽이라 스무 번을 돌려도 비용은 무시할 수 있다. */
    private static final int ATTEMPTS = 20;

    // 조건별로 "이 값이 같으면 같은 부류"를 정하는 기준.
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

        Map<Integer, List<Participant>> best = null;
        int bestGap = Integer.MAX_VALUE;
        int bestPenalty = Integer.MAX_VALUE;

        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            Map<Integer, List<Participant>> candidate = assignOnce(participants, teamCount, conditions, fixedMembers);
            int gap = sizeGap(candidate);
            int penalty = totalPenalty(candidate, conditions);

            // 인원 균등이 먼저다. 인원 분포가 같을 때에 한해 조건을 더 잘 만족한 쪽을 고른다.
            if (gap < bestGap || (gap == bestGap && penalty < bestPenalty)) {
                best = candidate;
                bestGap = gap;
                bestPenalty = penalty;
            }
        }
        return best;
    }

    // 그리디 한 번. 섞는 순서에 따라 매번 다른 편성이 나온다.
    private Map<Integer, List<Participant>> assignOnce(List<Participant> participants,
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

    // 벌점이 가장 낮은 조를 고른다. 동점이면 인원이 적은 조.
    private int bestTeam(Map<Integer, List<Participant>> teams,
                         Participant candidate,
                         Set<AssignmentCondition> conditions) {

        // 조 순회 순서도 섞는다. 벌점도 인원도 같은 조가 여럿일 때 항상 앞 번호가 뽑히지 않게 한다.
        List<Integer> order = new ArrayList<>(teams.keySet());
        Collections.shuffle(order);

        int bestTeam = -1;
        int bestPenalty = Integer.MAX_VALUE;
        int bestSize = Integer.MAX_VALUE;

        for (int teamNumber : order) {
            List<Participant> team = teams.get(teamNumber);
            int penalty = penaltyOf(team, candidate, conditions);
            int size = team.size();

            if (penalty < bestPenalty || (penalty == bestPenalty && size < bestSize)) {
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
            // 상수를 추가하고 KEYS에 넣지 않으면 조용히 무시되는 대신 여기서 바로 터진다
            Function<Participant, Object> key = Objects.requireNonNull(KEYS.get(condition),
                    () -> condition + "의 분류 기준이 없습니다.");

            Object candidateKey = key.apply(candidate);
            for (Participant member : team) {
                // major는 String, MBTI 키는 Boolean이라 ==로 비교하면 조용히 틀린다
                if (Objects.equals(key.apply(member), candidateKey)) penalty++;
            }
        }
        return penalty;
    }

    // 편성 전체의 벌점. 조마다 조원을 한 명씩 다시 넣어보며 같은 부류가 몇 쌍 겹쳤는지 센다.
    private int totalPenalty(Map<Integer, List<Participant>> teams, Set<AssignmentCondition> conditions) {
        int total = 0;
        for (List<Participant> team : teams.values()) {
            List<Participant> placed = new ArrayList<>(team.size());
            for (Participant member : team) {
                total += penaltyOf(placed, member, conditions);
                placed.add(member);
            }
        }
        return total;
    }

    // 가장 많은 조와 가장 적은 조의 인원 차이.
    private int sizeGap(Map<Integer, List<Participant>> teams) {
        var sizes = teams.values().stream().mapToInt(List::size).summaryStatistics();
        return sizes.getMax() - sizes.getMin();
    }
}
