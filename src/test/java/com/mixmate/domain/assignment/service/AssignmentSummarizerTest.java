package com.mixmate.domain.assignment.service;

import com.mixmate.domain.assignment.enums.AssignmentCondition;
import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.entity.ParticipantProfile;
import com.mixmate.domain.participant.enums.Gender;
import com.mixmate.domain.participant.enums.Grade;
import com.mixmate.domain.participant.enums.Mbti;
import com.mixmate.domain.participant.enums.Position;
import com.mixmate.domain.participant.enums.Visibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 편성 결과를 직접 만들어 넣고 어떤 경고가 나오는지만 검증합니다.
 * TeamAssigner를 거치면 결과가 매번 달라져 무엇을 단정할 수 없으므로, 조 구성을 손으로 짜서 넘깁니다.
 *
 * 참가자는 프로필이 모두 같고(3학년 / 컴퓨터공학과 / ISTP / 일반 참가자 / 기존 참가자 / 남성)
 * 각 테스트가 필요한 항목 하나만 바꿉니다.
 */
class AssignmentSummarizerTest {

    private final AssignmentSummarizer summarizer = new AssignmentSummarizer();

    @Test
    @DisplayName("남녀가 조마다 고르게 있으면 경고가 없다")
    void reportsNothingWhenEvenlySpread() {
        Map<Integer, List<Participant>> teams = teams(
                List.of(male(), female()),
                List.of(male(), female()));

        assertThat(summarizer.warningsOf(teams, Set.of(AssignmentCondition.GENDER_BALANCE))).isEmpty();
    }

    @Test
    @DisplayName("한 조에 남성이 몰리면 조건을 못 지켰다고 경고한다")
    void warnsWhenConditionIsBroken() {
        Map<Integer, List<Participant>> teams = teams(
                List.of(male(), male(), male()),
                List.of(female(), female(), female()));

        assertThat(summarizer.warningsOf(teams, Set.of(AssignmentCondition.GENDER_BALANCE)))
                .containsExactly("성별 균형이 지켜지지 않았습니다.");
    }

    @Test
    @DisplayName("인원이 나누어떨어지지 않아 생긴 한 명 차이는 경고하지 않는다")
    void toleratesUnavoidableOneOffDifference() {
        Map<Integer, List<Participant>> teams = teams(
                List.of(male(), male(), female()),
                List.of(male(), female()));

        assertThat(summarizer.warningsOf(teams, Set.of(AssignmentCondition.GENDER_BALANCE))).isEmpty();
    }

    @Test
    @DisplayName("운영진이 조 개수보다 적으면 조를 다 채울 수 없다고 경고한다")
    void warnsWhenValueCannotCoverEveryTeam() {
        Map<Integer, List<Participant>> teams = teams(
                List.of(staff(), member()),
                List.of(member(), member()),
                List.of(member(), member()));

        // 한 명을 세 조에 나눌 방법은 없으므로 편차 1은 최선이다. 못 지켰다는 경고는 나오지 않는다.
        assertThat(summarizer.warningsOf(teams, Set.of(AssignmentCondition.POSITION_SPREAD)))
                .containsExactly("운영진 1명으로는 3개 조를 모두 채울 수 없습니다.");
    }

    @Test
    @DisplayName("참가자가 전원 같은 값이면 균형을 맞출 수 없다고 경고한다")
    void warnsWhenOnlyOneValueExists() {
        Map<Integer, List<Participant>> teams = teams(
                List.of(male(), male()),
                List.of(male(), male()));

        assertThat(summarizer.warningsOf(teams, Set.of(AssignmentCondition.GENDER_BALANCE)))
                .containsExactly("남성만 있어 균형을 맞출 수 없습니다.");
    }

    @Test
    @DisplayName("학년처럼 값이 많은 조건은 조를 다 채우지 못해도 경고하지 않는다")
    void doesNotWarnAboutShortageForConditionsWithManyValues() {
        // 네 학년이 조마다 두 개씩만 들어가지만, 값이 많은 조건은 그것이 정상이라 경고 대상이 아니다
        Map<Integer, List<Participant>> teams = teams(
                List.of(grade(Grade.FIRST), grade(Grade.SECOND)),
                List.of(grade(Grade.THIRD), grade(Grade.FOURTH)));

        assertThat(summarizer.warningsOf(teams, Set.of(AssignmentCondition.GRADE_SPREAD))).isEmpty();
    }

    @Test
    @DisplayName("켜지 않은 조건은 아무리 편성이 나빠도 경고하지 않는다")
    void ignoresConditionsThatAreOff() {
        Map<Integer, List<Participant>> teams = teams(
                List.of(male(), male(), male()),
                List.of(female(), female(), female()));

        assertThat(summarizer.warningsOf(teams, Set.of())).isEmpty();
    }

    @Test
    @DisplayName("60명 6조에서 남녀가 조마다 5명씩이면 경고가 없다")
    void handlesLargeRosterEvenly() {
        Map<Integer, List<Participant>> teams = new LinkedHashMap<>();
        for (int teamNumber = 1; teamNumber <= 6; teamNumber++) {
            List<Participant> team = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                team.add(male());
                team.add(female());
            }
            teams.put(teamNumber, team);
        }

        assertThat(summarizer.warningsOf(teams, Set.of(AssignmentCondition.GENDER_BALANCE))).isEmpty();
    }

    @Test
    @DisplayName("60명 6조에서 남녀 30명씩인데 조별로 갈리면 못 지켰다고만 경고한다")
    void warnsOnlyAboutTheResultWhenRosterIsFine() {
        Map<Integer, List<Participant>> teams = new LinkedHashMap<>();
        for (int teamNumber = 1; teamNumber <= 6; teamNumber++) {
            teams.put(teamNumber, teamNumber <= 3 ? fill(10, this::female) : fill(10, this::male));
        }

        // 성비는 완벽하므로 인원 부족 경고는 없고, 편성이 갈렸다는 경고만 나온다
        assertThat(summarizer.warningsOf(teams, Set.of(AssignmentCondition.GENDER_BALANCE)))
                .containsExactly("성별 균형이 지켜지지 않았습니다.");
    }

    @Test
    @DisplayName("신입이 한 명뿐이면 편성은 최선이어도 조를 다 채울 수 없다고 경고한다")
    void warnsAboutShortageEvenWhenAssignmentIsBest() {
        Map<Integer, List<Participant>> teams = new LinkedHashMap<>();
        teams.put(1, List.of(newcomer(), male()));
        for (int teamNumber = 2; teamNumber <= 10; teamNumber++) {
            teams.put(teamNumber, fill(2, this::male));
        }

        assertThat(summarizer.warningsOf(teams, Set.of(AssignmentCondition.NEWCOMER_SPREAD)))
                .containsExactly("신입 1명으로는 10개 조를 모두 채울 수 없습니다.");
    }

    @Test
    @DisplayName("조건을 전부 켜면 경고가 조건 선언 순서대로 쌓인다")
    void ordersWarningsByConditionDeclaration() {
        Map<Integer, List<Participant>> teams = teams(
                List.of(male(), female()),
                List.of(male(), female()));

        // 성별만 섞여 있고 나머지 항목은 전원 같은 값이라 세 조건에서 경고가 나온다
        assertThat(summarizer.warningsOf(teams, EnumSet.allOf(AssignmentCondition.class)))
                .containsExactly(
                        "내향형(I)만 있어 균형을 맞출 수 없습니다.",
                        "기존 참가자만 있어 균형을 맞출 수 없습니다.",
                        "일반 참가자만 있어 균형을 맞출 수 없습니다.");
    }

    @Test
    @DisplayName("빈 조가 섞여 있어도 계산이 되고 그 편차가 경고로 잡힌다")
    void handlesEmptyTeam() {
        Map<Integer, List<Participant>> teams = teams(
                List.of(male(), female(), male(), female()),
                List.of());

        assertThat(summarizer.warningsOf(teams, Set.of(AssignmentCondition.GENDER_BALANCE)))
                .containsExactly("성별 균형이 지켜지지 않았습니다.");
    }

    @Test
    @DisplayName("참가자가 한 명뿐이어도 터지지 않는다")
    void handlesSingleParticipant() {
        Map<Integer, List<Participant>> teams = teams(
                List.of(male()),
                List.of());

        assertThat(summarizer.warningsOf(teams, Set.of(AssignmentCondition.GENDER_BALANCE)))
                .containsExactly("남성만 있어 균형을 맞출 수 없습니다.");
    }

    @Test
    @DisplayName("100명 10조에서 학년이 고르게 흩어져 있으면 경고가 없다")
    void handlesManyDistinctValues() {
        Map<Integer, List<Participant>> teams = new LinkedHashMap<>();
        int seq = 0;
        for (int teamNumber = 1; teamNumber <= 10; teamNumber++) {
            List<Participant> team = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                team.add(grade(Grade.values()[seq++ % Grade.values().length]));
            }
            teams.put(teamNumber, team);
        }

        assertThat(summarizer.warningsOf(teams, Set.of(AssignmentCondition.GRADE_SPREAD))).isEmpty();
    }

    // --- helpers ---

    private List<Participant> fill(int count, Supplier<Participant> factory) {
        return IntStream.range(0, count).mapToObj(i -> factory.get()).collect(Collectors.toList());
    }

    @SafeVarargs
    private Map<Integer, List<Participant>> teams(List<Participant>... members) {
        Map<Integer, List<Participant>> teams = new LinkedHashMap<>();
        for (int i = 0; i < members.length; i++) {
            teams.put(i + 1, members[i]);
        }
        return teams;
    }

    private Participant male() {
        return participant(Gender.MALE, Position.MEMBER, "컴퓨터공학과", false);
    }

    private Participant female() {
        return participant(Gender.FEMALE, Position.MEMBER, "컴퓨터공학과", false);
    }

    private Participant staff() {
        return participant(Gender.MALE, Position.STAFF, "컴퓨터공학과", false);
    }

    private Participant member() {
        return participant(Gender.MALE, Position.MEMBER, "컴퓨터공학과", false);
    }

    private Participant newcomer() {
        return participant(Gender.MALE, Position.MEMBER, "컴퓨터공학과", true);
    }

    private Participant grade(Grade grade) {
        return participant(Gender.MALE, Position.MEMBER, "컴퓨터공학과", false, grade);
    }

    /** 경고는 프로필만 보고 계산하므로 영속화도 식별자도 필요 없다. */
    private Participant participant(Gender gender, Position position, String major, boolean isNew) {
        return participant(gender, position, major, isNew, Grade.THIRD);
    }

    private Participant participant(Gender gender, Position position, String major, boolean isNew, Grade grade) {
        ParticipantProfile profile = ParticipantProfile.builder()
                .displayName("참가자")
                .position(position)
                .major(major)
                .isNew(isNew)
                .grade(grade)
                .gender(gender)
                .mbti(Mbti.ISTP)
                .age(24)
                .visibility(Visibility.PUBLIC)
                .build();

        return Participant.addByHost(null, profile);
    }
}
