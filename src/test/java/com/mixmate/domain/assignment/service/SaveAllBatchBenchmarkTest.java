package com.mixmate.domain.assignment.service;

import com.mixmate.domain.assignment.entity.GroupAssignment;
import com.mixmate.domain.assignment.entity.TeamAssignmentMember;
import com.mixmate.domain.assignment.enums.AssignmentCondition;
import com.mixmate.domain.assignment.repository.GroupAssignmentRepository;
import com.mixmate.domain.assignment.repository.TeamAssignmentMemberRepository;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.group.repository.GroupRepository;
import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.entity.ParticipantProfile;
import com.mixmate.domain.participant.enums.Gender;
import com.mixmate.domain.participant.enums.Grade;
import com.mixmate.domain.participant.enums.Mbti;
import com.mixmate.domain.participant.enums.Position;
import com.mixmate.domain.participant.enums.Round;
import com.mixmate.domain.participant.enums.Visibility;
import com.mixmate.domain.participant.repository.ParticipantRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * saveAll()이 IDENTITY 전략 때문에 실제로는 배치 처리가 안 된다는 걸 확인하기 위한 벤치마크.
 *
 * 정상 동작을 검증하는 테스트가 아니라 수치만 콘솔에 찍는 일회성 조사 코드라 "benchmark" 태그를 달아
 * 평소 ./gradlew test 실행에서는 빠지고, 필요할 때 --tests로 이름을 지정해야만 실행된다.
 * {@code @Transactional}이라 끝나면 자동 롤백되어 로컬 DB에 데이터가 남지 않는다.
 */
@Tag("benchmark")
@SpringBootTest
@Transactional
class SaveAllBatchBenchmarkTest {

    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private GroupAssignmentRepository groupAssignmentRepository;
    @Autowired
    private ParticipantRepository participantRepository;
    @Autowired
    private TeamAssignmentMemberRepository teamAssignmentMemberRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("100건/500건 규모에서 개별 insert(JPA saveAll) vs 배치 insert(JdbcTemplate) 시간 비교")
    void compareIndividualVsBatchInsert() {
        for (int rows : List.of(100, 500)) {
            // group_assignment는 (group_id, round) 유니크라, 규모별로 매번 새 그룹을 만들어 충돌을 피한다.
            Group group = groupRepository.save(Group.create("벤치마크용 그룹 " + rows, null, "BENCH" + rows, "tokenBench" + rows));

            List<Participant> participants = new ArrayList<>();
            for (int i = 0; i < rows; i++) {
                ParticipantProfile profile = ParticipantProfile.builder()
                        .displayName("참가자" + i)
                        .studentId("2024" + String.format("%05d", i))
                        .position(Position.MEMBER)
                        .major("컴퓨터공학과")
                        .isNew(false)
                        .grade(Grade.THIRD)
                        .gender(i % 2 == 0 ? Gender.MALE : Gender.FEMALE)
                        .mbti(Mbti.ENFP)
                        .visibility(Visibility.PUBLIC)
                        .build();
                participants.add(Participant.addByHost(group, profile));
            }
            participantRepository.saveAll(participants);
            em.flush();

            GroupAssignment individualAssignment = groupAssignmentRepository.save(
                    GroupAssignment.create(group, Round.FIRST_ROUND, 5, EnumSet.noneOf(AssignmentCondition.class)));
            em.flush();

            long individualStart = System.nanoTime();
            List<TeamAssignmentMember> individualMembers = new ArrayList<>();
            for (int i = 0; i < rows; i++) {
                individualMembers.add(TeamAssignmentMember.create(individualAssignment, participants.get(i), i % 5, false));
            }
            teamAssignmentMemberRepository.saveAll(individualMembers);
            em.flush();
            double individualMs = (System.nanoTime() - individualStart) / 1_000_000.0;

            GroupAssignment batchAssignment = groupAssignmentRepository.save(
                    GroupAssignment.create(group, Round.SECOND_ROUND, 5, EnumSet.noneOf(AssignmentCondition.class)));
            em.flush();

            long batchStart = System.nanoTime();
            jdbcTemplate.batchUpdate(
                    "INSERT INTO team_assignment_member (assignment_id, participant_id, team_number, is_fixed) VALUES (?, ?, ?, ?)",
                    participants,
                    rows,
                    (ps, participant) -> {
                        ps.setLong(1, batchAssignment.getAssignmentId());
                        ps.setLong(2, participant.getParticipantId());
                        ps.setInt(3, 0);
                        ps.setBoolean(4, false);
                    });
            double batchMs = (System.nanoTime() - batchStart) / 1_000_000.0;

            System.out.printf("[BENCH] rows=%d individual(JPA saveAll)=%sms batch(JdbcTemplate)=%sms%n",
                    rows, individualMs, batchMs);
        }
    }
}
