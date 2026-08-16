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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TeamAssigner는 스프링도 DB도 타지 않는 순수 계산 클래스라 new로 바로 검증합니다.
 *
 * 결과가 매번 달라지는 알고리즘이라 "어느 조에 누가 갔는지"는 단정할 수 없습니다.
 * 대신 조건별로 보장되어야 하는 분포(같은 부류가 한 조에 몰리지 않는다)를 확인합니다.
 */
class TeamAssignerTest {

    private final TeamAssigner assigner = new TeamAssigner();

    private long nextId = 1L;

    @Test
    @DisplayName("조건이 없어도 모든 참가자가 정확히 한 조에만 배정된다")
    void assignsEveryParticipantExactlyOnce() {
        List<Participant> participants = participants(30, i -> basic());

        Map<Integer, List<Participant>> teams = assigner.assign(participants, 3, Set.of(), Map.of());

        assertThat(teams.keySet()).containsExactly(1, 2, 3);
        assertThat(flatten(teams)).containsExactlyInAnyOrderElementsOf(participants);
    }

    @Test
    @DisplayName("조건을 켜지 않아도 조별 인원 차이가 1 이하다")
    void balancesTeamSize() {
        List<Participant> participants = participants(31, i -> basic());

        Map<Integer, List<Participant>> teams = assigner.assign(participants, 3, Set.of(), Map.of());

        assertThat(spread(teams, p -> true)).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("조건이 없어 벌점이 늘 0이어도 빈 조가 생기지 않는다")
    void neverLeavesTeamEmpty() {
        List<Participant> participants = participants(6, i -> basic());

        // 인원이 적을수록 한쪽으로 몰리기 쉬워 한 번만 돌려서는 드러나지 않는다
        for (int i = 0; i < 100; i++) {
            Map<Integer, List<Participant>> teams = assigner.assign(participants, 3, Set.of(), Map.of());

            assertThat(teams.values()).allSatisfy(team -> assertThat(team).isNotEmpty());
        }
    }

    @Test
    @DisplayName("학과가 전원 달라 어느 조에 넣어도 벌점이 0이어도 인원은 고르게 나뉜다")
    void balancesWhenNoParticipantSharesKey() {
        List<Participant> participants = participants(12, i -> withMajor("학과" + i));

        Map<Integer, List<Participant>> teams =
                assigner.assign(participants, 3, Set.of(AssignmentCondition.MAJOR_SPREAD), Map.of());

        assertThat(spread(teams, p -> true)).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("성별 균형을 켜면 조별 남성 수 차이가 1 이하다")
    void balancesGender() {
        List<Participant> participants = new ArrayList<>();
        participants.addAll(participants(15, i -> withGender(Gender.MALE)));
        participants.addAll(participants(15, i -> withGender(Gender.FEMALE)));

        Map<Integer, List<Participant>> teams =
                assigner.assign(participants, 3, Set.of(AssignmentCondition.GENDER_BALANCE), Map.of());

        assertThat(spread(teams, p -> p.getProfile().getGender() == Gender.MALE)).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("MBTI 균형은 16개 유형이 아니라 E/I만 본다")
    void balancesExtroversionOnly() {
        List<Participant> participants = new ArrayList<>();
        // 같은 E라도 유형은 제각각이다. 유형 전체로 비교하면 이 셋이 서로 다른 부류가 되어 균형이 깨진다.
        participants.addAll(participants(5, i -> withMbti(Mbti.ENFP)));
        participants.addAll(participants(5, i -> withMbti(Mbti.ESTJ)));
        participants.addAll(participants(5, i -> withMbti(Mbti.ESFP)));
        participants.addAll(participants(15, i -> withMbti(Mbti.ISTP)));

        Map<Integer, List<Participant>> teams =
                assigner.assign(participants, 3, Set.of(AssignmentCondition.MBTI_BALANCE), Map.of());

        assertThat(spread(teams, p -> p.getProfile().getMbti().isExtrovert())).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("직급 분산을 켜면 소수인 운영진이 조마다 한 명씩 흩어진다")
    void spreadsStaffAcrossTeams() {
        List<Participant> participants = new ArrayList<>();
        participants.addAll(participants(3, i -> withPosition(Position.STAFF)));
        participants.addAll(participants(12, i -> withPosition(Position.MEMBER)));

        Map<Integer, List<Participant>> teams =
                assigner.assign(participants, 3, Set.of(AssignmentCondition.POSITION_SPREAD), Map.of());

        assertThat(teams.values())
                .allSatisfy(team -> assertThat(count(team, p -> p.getProfile().getPosition() == Position.STAFF))
                        .isEqualTo(1));
    }

    @Test
    @DisplayName("고정 멤버는 지정한 조에 그대로 배정된다")
    void keepsFixedMembers() {
        List<Participant> participants = participants(12, i -> basic());
        Map<Long, Integer> fixedMembers = Map.of(
                participants.get(0).getParticipantId(), 3,
                participants.get(1).getParticipantId(), 2);

        Map<Integer, List<Participant>> teams = assigner.assign(participants, 3, Set.of(), fixedMembers);

        assertThat(teams.get(3)).contains(participants.get(0));
        assertThat(teams.get(2)).contains(participants.get(1));
        assertThat(flatten(teams)).containsExactlyInAnyOrderElementsOf(participants);
    }

    @Test
    @DisplayName("전원이 같은 값이라 어느 조를 골라도 벌점이 같으면 인원만 고르게 나눈다")
    void handlesAllIdenticalParticipants() {
        List<Participant> participants = participants(12, i -> basic());

        Map<Integer, List<Participant>> teams =
                assigner.assign(participants, 3, Set.of(AssignmentCondition.MAJOR_SPREAD), Map.of());

        assertThat(flatten(teams)).containsExactlyInAnyOrderElementsOf(participants);
        assertThat(spread(teams, p -> true)).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("같은 입력으로 여러 번 실행하면 매번 같은 편성이 나오지는 않는다")
    void producesDifferentResultsAcrossRuns() {
        List<Participant> participants = participants(12, i -> basic());

        Set<String> results = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            results.add(signature(assigner.assign(participants, 3, Set.of(), Map.of())));
        }

        assertThat(results).hasSizeGreaterThan(1);
    }

    // --- helpers ---

    /** 조별로 조건에 해당하는 인원을 세서, 가장 많은 조와 가장 적은 조의 차이를 돌려준다. */
    private int spread(Map<Integer, List<Participant>> teams, Predicate<Participant> predicate) {
        var stats = teams.values().stream().mapToInt(team -> count(team, predicate)).summaryStatistics();
        return stats.getMax() - stats.getMin();
    }

    private int count(List<Participant> team, Predicate<Participant> predicate) {
        return (int) team.stream().filter(predicate).count();
    }

    private List<Participant> flatten(Map<Integer, List<Participant>> teams) {
        return teams.values().stream().flatMap(List::stream).toList();
    }

    /** 편성 결과를 문자열로 눌러 담아 실행 간 비교에 쓴다. */
    private String signature(Map<Integer, List<Participant>> teams) {
        return teams.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue().stream()
                        .map(Participant::getParticipantId)
                        .sorted()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")))
                .collect(Collectors.joining("|"));
    }

    private List<Participant> participants(int count, java.util.function.IntFunction<Participant> factory) {
        return IntStream.range(0, count).mapToObj(factory).collect(Collectors.toList());
    }

    private Participant basic() {
        return participant(Gender.MALE, Grade.THIRD, Position.MEMBER, Mbti.ISTP, "컴퓨터공학과", false);
    }

    private Participant withGender(Gender gender) {
        return participant(gender, Grade.THIRD, Position.MEMBER, Mbti.ISTP, "컴퓨터공학과", false);
    }

    private Participant withMbti(Mbti mbti) {
        return participant(Gender.MALE, Grade.THIRD, Position.MEMBER, mbti, "컴퓨터공학과", false);
    }

    private Participant withMajor(String major) {
        return participant(Gender.MALE, Grade.THIRD, Position.MEMBER, Mbti.ISTP, major, false);
    }

    private Participant withPosition(Position position) {
        return participant(Gender.MALE, Grade.THIRD, position, Mbti.ISTP, "컴퓨터공학과", false);
    }

    /**
     * 영속화하지 않으므로 participantId가 null로 남는다.
     * 고정 멤버 조회가 id를 키로 쓰기 때문에 리플렉션으로 직접 채워 넣는다.
     */
    private Participant participant(Gender gender, Grade grade, Position position,
                                    Mbti mbti, String major, boolean isNew) {
        ParticipantProfile profile = ParticipantProfile.builder()
                .displayName("참가자" + nextId)
                .position(position)
                .major(major)
                .isNew(isNew)
                .grade(grade)
                .gender(gender)
                .mbti(mbti)
                .age(24)
                .visibility(Visibility.PUBLIC)
                .build();

        Participant participant = Participant.addByHost(null, profile);
        ReflectionTestUtils.setField(participant, "participantId", nextId++);
        return participant;
    }
}
