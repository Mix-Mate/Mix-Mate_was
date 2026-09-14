package com.mixmate.domain.participant.service;

import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.group.repository.GroupBanRepository;
import com.mixmate.domain.group.repository.GroupRepository;
import com.mixmate.domain.participant.dto.RosterMember;
import com.mixmate.domain.participant.dto.RosterRound;
import com.mixmate.domain.participant.dto.response.RosterResponse;
import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.entity.ParticipantProfile;
import com.mixmate.domain.participant.enums.*;
import com.mixmate.domain.participant.repository.ParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * getRoster가 rounds에 어떤 차수를 담는지만 확인합니다.
 *
 * 관리자 검증은 GroupMembership이, 2차 참여 8명 제한은 GroupService.decideSecondRound가 각각 강제하므로
 * 여기서는 둘 다 검증하지 않습니다. 이 테스트가 다루는 것은 "그래서 명단에 2차를 담느냐"뿐입니다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ParticipantServiceRosterTest {

    private static final long GROUP_ID = 1L;
    private static final long HOST_USER_ID = 100L;

    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupBanRepository groupBanRepository;
    @Mock
    private GroupMembership groupMembership;
    @Mock
    private AssignmentReset assignmentReset;
    @Mock
    private TeamNumberLookup teamNumberLookup;

    @InjectMocks
    private ParticipantService participantService;

    private Group group;
    private long nextParticipantId;

    @BeforeEach
    void setUp() {
        group = Group.create("신촌 모임", "1차 술자리", "PTC12345", "tokenRosterAAAAAAAAAAA");
        nextParticipantId = 1L;

        Participant host = participant("김대현", RoundParticipation.FIRST_ONLY);
        when(groupMembership.getHost(GROUP_ID, HOST_USER_ID)).thenReturn(host);

        // 편성이 없는 것이 기본값. 편성이 있는 케이스만 각 테스트에서 덮어쓴다.
        when(teamNumberLookup.teamNumbersOf(group, Round.FIRST_ROUND)).thenReturn(Map.of());
        when(teamNumberLookup.teamNumbersOf(group, Round.SECOND_ROUND)).thenReturn(Map.of());
    }

    @Test
    @DisplayName("모집 중이면 1차만 담기고, 조 번호는 전원 null이다")
    void recruiting() {
        givenParticipants(members(3, RoundParticipation.FIRST_ONLY));

        RosterResponse response = participantService.getRoster(GROUP_ID, HOST_USER_ID);

        assertThat(response.groupName()).isEqualTo("신촌 모임");
        assertThat(response.rounds()).hasSize(1);

        RosterRound firstRound = response.rounds().get(0);
        assertThat(firstRound.round()).isEqualTo(Round.FIRST_ROUND);
        assertThat(firstRound.assigned()).isFalse();
        assertThat(firstRound.members()).hasSize(3)
                .extracting(RosterMember::teamNumber).containsOnlyNulls();
    }

    @Test
    @DisplayName("2차 참여자가 8명 미만이면 투표 중이어도 2차는 담기지 않는다")
    void votingBelowMinimum() {
        group.closeRecruiting();
        group.startRound(Round.FIRST_ROUND);
        group.startVoting();
        givenParticipants(mixed(5, 3));

        RosterResponse response = participantService.getRoster(GROUP_ID, HOST_USER_ID);

        assertThat(response.rounds()).extracting(RosterRound::round).containsExactly(Round.FIRST_ROUND);
    }

    @Test
    @DisplayName("2차 참여자가 8명을 넘겨도 관리자가 2차 없이 종료했으면 2차는 담기지 않는다")
    void finishedWithoutSecondRound() {
        group.closeRecruiting();
        group.startRound(Round.FIRST_ROUND);
        group.startVoting();
        group.finishVoting();
        group.finish();
        givenParticipants(mixed(2, 10));

        RosterResponse response = participantService.getRoster(GROUP_ID, HOST_USER_ID);

        assertThat(response.rounds()).extracting(RosterRound::round).containsExactly(Round.FIRST_ROUND);
    }

    @Test
    @DisplayName("2차가 확정되면 편성 전이라도 2차가 담기고, assigned는 false다")
    void beforeSecondRound() {
        group.closeRecruiting();
        group.startRound(Round.FIRST_ROUND);
        group.startVoting();
        group.finishVoting();
        group.decideSecondRound();
        List<Participant> participants = mixed(2, 8);
        givenParticipants(participants);

        RosterResponse response = participantService.getRoster(GROUP_ID, HOST_USER_ID);

        assertThat(response.rounds()).hasSize(2);
        RosterRound secondRound = response.rounds().get(1);
        assertThat(secondRound.round()).isEqualTo(Round.SECOND_ROUND);
        assertThat(secondRound.assigned()).isFalse();
        assertThat(secondRound.members()).hasSize(8)
                .extracting(RosterMember::teamNumber).containsOnlyNulls();
    }

    @Test
    @DisplayName("2차 편성이 있으면 2차가 담기고 조 번호가 채워진다")
    void secondRoundAssigned() {
        group.closeRecruiting();
        group.startRound(Round.FIRST_ROUND);
        group.startVoting();
        group.finishVoting();
        group.decideSecondRound();
        group.startRound(Round.SECOND_ROUND);

        List<Participant> participants = mixed(0, 4);
        givenParticipants(participants);
        when(teamNumberLookup.teamNumbersOf(group, Round.SECOND_ROUND)).thenReturn(Map.of(
                participants.get(0).getParticipantId(), 1,
                participants.get(1).getParticipantId(), 2,
                participants.get(2).getParticipantId(), 1,
                participants.get(3).getParticipantId(), 2));

        RosterResponse response = participantService.getRoster(GROUP_ID, HOST_USER_ID);

        RosterRound secondRound = response.rounds().get(1);
        assertThat(secondRound.assigned()).isTrue();
        assertThat(secondRound.members())
                .extracting(RosterMember::teamNumber).containsExactly(1, 2, 1, 2);
    }

    @Test
    @DisplayName("1차 편성이 있으면 1차 명단에 조 번호가 채워진다")
    void firstRoundAssigned() {
        group.closeRecruiting();
        group.startRound(Round.FIRST_ROUND);

        List<Participant> participants = members(2, RoundParticipation.FIRST_ONLY);
        givenParticipants(participants);
        when(teamNumberLookup.teamNumbersOf(group, Round.FIRST_ROUND)).thenReturn(Map.of(
                participants.get(0).getParticipantId(), 1,
                participants.get(1).getParticipantId(), 2));

        RosterResponse response = participantService.getRoster(GROUP_ID, HOST_USER_ID);

        RosterRound firstRound = response.rounds().get(0);
        assertThat(firstRound.assigned()).isTrue();
        assertThat(firstRound.members())
                .extracting(RosterMember::teamNumber).containsExactly(1, 2);
    }

    @Test
    @DisplayName("2차 명단은 쿼리를 새로 치지 않고 1차 명단에서 걸러낸다")
    void secondRoundReusesFirstRoundQuery() {
        group.closeRecruiting();
        group.startRound(Round.FIRST_ROUND);
        group.startVoting();
        group.finishVoting();
        group.decideSecondRound();
        givenParticipants(mixed(3, 8));

        participantService.getRoster(GROUP_ID, HOST_USER_ID);

        verify(participantRepository).findByGroup(group);
        verify(participantRepository, never())
                .findByGroupAndRoundParticipation(group, RoundParticipation.FIRST_AND_SECOND);
    }

    @Test
    @DisplayName("명단에는 학번·이름·학과·학년·성별이 담긴다")
    void memberFields() {
        List<Participant> participants = members(1, RoundParticipation.FIRST_ONLY);
        givenParticipants(participants);

        RosterMember member = participantService.getRoster(GROUP_ID, HOST_USER_ID)
                .rounds().get(0).members().get(0);

        assertThat(member.studentId()).isEqualTo("2024001");
        assertThat(member.displayName())
                .isEqualTo(participants.get(0).getProfile().getDisplayName());
        assertThat(member.major()).isEqualTo("컴퓨터공학과");
        assertThat(member.grade()).isEqualTo(Grade.SECOND);
        assertThat(member.gender()).isEqualTo(Gender.MALE);
    }

    private void givenParticipants(List<Participant> participants) {
        when(participantRepository.findByGroup(group)).thenReturn(participants);
    }

    /** 1차만 참여하는 사람 firstOnly명과 2차까지 가는 사람 firstAndSecond명을 한 명단에 담는다. */
    private List<Participant> mixed(int firstOnly, int firstAndSecond) {
        List<Participant> participants = new ArrayList<>(members(firstOnly, RoundParticipation.FIRST_ONLY));
        participants.addAll(members(firstAndSecond, RoundParticipation.FIRST_AND_SECOND));
        return participants;
    }

    private List<Participant> members(int count, RoundParticipation participation) {
        List<Participant> participants = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            participants.add(participant("참가자" + nextParticipantId, participation));
        }
        return participants;
    }

    /** participantId는 DB가 채우는 값이라 조 번호 매핑을 확인하려면 테스트가 직접 심어야 한다. */
    private Participant participant(String displayName, RoundParticipation participation) {
        ParticipantProfile profile = ParticipantProfile.builder()
                .displayName(displayName)
                .studentId("2024001")
                .position(Position.MEMBER)
                .major("컴퓨터공학과")
                .isNew(false)
                .grade(Grade.SECOND)
                .gender(Gender.MALE)
                .mbti(Mbti.ISTJ)
                .visibility(Visibility.PUBLIC)
                .build();

        Participant participant = Participant.addByHost(group, profile);
        if (participation == RoundParticipation.FIRST_AND_SECOND) {
            participant.joinSecondRound();
        }
        ReflectionTestUtils.setField(participant, "participantId", nextParticipantId++);
        return participant;
    }
}
