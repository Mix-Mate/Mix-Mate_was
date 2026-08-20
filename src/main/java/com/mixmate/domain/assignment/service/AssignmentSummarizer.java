package com.mixmate.domain.assignment.service;

import com.mixmate.domain.assignment.enums.AssignmentCondition;
import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.enums.Gender;
import com.mixmate.domain.participant.enums.Position;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * 편성 결과에서 관리자가 확정 전에 알아야 할 것을 문장으로 뽑아냅니다.
 *
 * 두 가지를 같은 목록에 담습니다. 켠 조건을 못 지킨 것과, 참가자 구성상 애초에 지킬 수 없는 것입니다.
 * 관리자 화면에서는 둘 다 "편성 경고"로 함께 보여주고, 재편성할지 그대로 확정할지 판단하는 근거로 씁니다.
 *
 * TeamAssigner와 마찬가지로 저장도 검증도 하지 않는 순수 계산 클래스입니다.
 */
@Component
public class AssignmentSummarizer {

    // 인원이 조 개수로 나누어떨어지지 않으면 한 명 차이는 피할 수 없다. 2 이상 벌어졌을 때만 못 지킨 것으로 본다.
    private static final int ALLOWED_SPREAD = 1;

    // 값이 두 가지뿐이라 "모든 조에 양쪽이 다 있어야" 의미가 사는 조건들.
    // 학년은 값이 많아 한 조에 특정 값이 없는 게 정상이므로 인원 부족을 따지지 않는다.
    private static final Set<AssignmentCondition> COVERAGE_CONDITIONS = EnumSet.of(
            AssignmentCondition.GENDER_BALANCE,
            AssignmentCondition.MBTI_BALANCE,
            AssignmentCondition.NEWCOMER_SPREAD,
            AssignmentCondition.POSITION_SPREAD);

    /** 경고가 없으면 빈 목록을 돌려줍니다. 순서는 조건 선언 순서를 따르므로 같은 편성이면 항상 같습니다. */
    public List<String> warningsOf(Map<Integer, List<Participant>> teams,
                                   Set<AssignmentCondition> conditions) {

        int teamCount = teams.size();
        List<Participant> everyone = teams.values().stream().flatMap(List::stream).toList();

        List<String> warnings = new ArrayList<>();
        for (AssignmentCondition condition : AssignmentCondition.values()) {
            if (!conditions.contains(condition)) continue;

            Function<Participant, Object> key = ParticipantKeys.of(condition);

            if (!isSatisfied(teams, key)) {
                // 조건 이름은 모두 "균형" 또는 "분산"으로 끝나 받침이 있으므로 조사는 "이"로 고정된다
                warnings.add("%s이 지켜지지 않았습니다.".formatted(conditionLabel(condition)));
            }
            if (COVERAGE_CONDITIONS.contains(condition)) {
                warnings.addAll(shortageOf(condition, key, everyone, teamCount));
            }
        }
        return warnings;
    }

    // 값마다 따로 본다. 하나라도 크게 몰렸으면 그 조건은 못 지킨 것이다.
    private boolean isSatisfied(Map<Integer, List<Participant>> teams, Function<Participant, Object> key) {
        for (Object value : distinctValues(teams, key)) {
            if (spread(teams.values(), team -> count(team, key, value)) > ALLOWED_SPREAD) return false;
        }
        return true;
    }

    /**
     * 참가자 구성 자체가 조건을 만족시킬 수 없는 경우를 찾는다.
     * 예를 들어 운영진이 두 명뿐인데 조가 다섯 개면 어떻게 나눠도 세 조에는 운영진이 없다.
     */
    private List<String> shortageOf(AssignmentCondition condition, Function<Participant, Object> key,
                                    List<Participant> everyone, int teamCount) {

        Map<Object, Long> countByValue = everyone.stream()
                .collect(Collectors.groupingBy(key, Collectors.counting()));

        // 한쪽 값만 있으면 groupingBy에 반대쪽이 아예 안 잡히므로 인원을 세기 전에 따로 거른다
        if (countByValue.size() == 1) {
            Object only = countByValue.keySet().iterator().next();
            // 받침에 따라 조사가 갈리지 않도록 "이라/라"를 쓰지 않는다
            return List.of("%s만 있어 균형을 맞출 수 없습니다.".formatted(valueLabel(condition, only)));
        }

        List<String> shortages = new ArrayList<>();
        countByValue.forEach((value, count) -> {
            if (count < teamCount) {
                // 조사를 붙이면 받침에 따라 이/가가 갈리므로 조사 없는 문장으로 만든다
                shortages.add("%s %d명으로는 %d개 조를 모두 채울 수 없습니다."
                        .formatted(valueLabel(condition, value), count, teamCount));
            }
        });
        return shortages;
    }

    private Set<Object> distinctValues(Map<Integer, List<Participant>> teams, Function<Participant, Object> key) {
        return teams.values().stream().flatMap(List::stream).map(key).collect(Collectors.toSet());
    }

    // 가장 많은 조와 가장 적은 조의 차이. 무엇을 셀지는 넘겨받는다.
    private int spread(Collection<List<Participant>> teams, ToIntFunction<List<Participant>> counter) {
        IntSummaryStatistics stats = teams.stream().mapToInt(counter).summaryStatistics();
        return stats.getMax() - stats.getMin();
    }

    private int count(List<Participant> team, Function<Participant, Object> key, Object value) {
        return (int) team.stream().filter(p -> Objects.equals(key.apply(p), value)).count();
    }

    private String conditionLabel(AssignmentCondition condition) {
        return switch (condition) {
            case GENDER_BALANCE -> "성별 균형";
            case MBTI_BALANCE -> "MBTI 균형";
            case GRADE_SPREAD -> "학년 분산";
            case NEWCOMER_SPREAD -> "신입 분산";
            case POSITION_SPREAD -> "직급 분산";
        };
    }

    private String valueLabel(AssignmentCondition condition, Object value) {
        return switch (condition) {
            case GENDER_BALANCE -> value == Gender.MALE ? "남성" : "여성";
            case MBTI_BALANCE -> Boolean.TRUE.equals(value) ? "외향형(E)" : "내향형(I)";
            case NEWCOMER_SPREAD -> Boolean.TRUE.equals(value) ? "신입" : "기존 참가자";
            case POSITION_SPREAD -> value == Position.STAFF ? "운영진" : "일반 참가자";
            case GRADE_SPREAD -> String.valueOf(value);
        };
    }
}
