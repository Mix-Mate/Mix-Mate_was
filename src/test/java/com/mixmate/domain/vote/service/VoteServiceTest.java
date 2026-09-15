package com.mixmate.domain.vote.service;

import com.mixmate.domain.assignment.entity.GroupAssignment;
import com.mixmate.domain.assignment.entity.TeamAssignmentMember;
import com.mixmate.domain.assignment.enums.AssignmentCondition;
import com.mixmate.domain.assignment.repository.GroupAssignmentRepository;
import com.mixmate.domain.assignment.repository.TeamAssignmentMemberRepository;
import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.group.event.GroupStatusChangedEvent;
import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.entity.ParticipantProfile;
import com.mixmate.domain.participant.enums.Gender;
import com.mixmate.domain.participant.enums.Grade;
import com.mixmate.domain.participant.enums.Mbti;
import com.mixmate.domain.participant.enums.Position;
import com.mixmate.domain.participant.enums.Round;
import com.mixmate.domain.participant.enums.Visibility;
import com.mixmate.domain.participant.repository.ParticipantRepository;
import com.mixmate.domain.participant.service.GroupMembership;
import com.mixmate.domain.vote.dto.request.AdminRound2VoteReqDto;
import com.mixmate.domain.vote.dto.request.MvpVoteReqDto;
import com.mixmate.domain.vote.dto.request.Round2VoteReqDto;
import com.mixmate.domain.vote.dto.response.Round2VoteStatusResDto;
import com.mixmate.domain.vote.dto.response.VoteResultResDto;
import com.mixmate.domain.vote.entity.Round2ParticipationVote;
import com.mixmate.domain.vote.enums.VoteChoice;
import com.mixmate.domain.vote.repository.MvpVoteRepository;
import com.mixmate.domain.vote.repository.Round2ParticipationVoteRepository;
import com.mixmate.exception.CustomException;
import com.mixmate.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private MvpVoteRepository mvpVoteRepository;
    @Mock
    private Round2ParticipationVoteRepository round2VoteRepository;
    @Mock
    private GroupAssignmentRepository groupAssignmentRepository;
    @Mock
    private TeamAssignmentMemberRepository teamAssignmentMemberRepository;
    @Mock
    private GroupMembership groupMembership;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private VoteService voteService;

    private Group group;
    private long nextParticipantId = 1L;

    @BeforeEach
    void setUp() {
        group = Group.create("테스트 모임", null, "TEST1234");
        group.startVoting();
    }

    private Participant participant(boolean withUser) {
        ParticipantProfile profile = ParticipantProfile.builder()
                .displayName("참가자").studentId("2024001").position(Position.MEMBER)
                .major("컴퓨터공학과").isNew(false).grade(Grade.SECOND).gender(Gender.MALE)
                .mbti(Mbti.ISTJ).visibility(Visibility.PUBLIC).build();
        User user = withUser ? User.builder().userId(nextParticipantId).email("u" + nextParticipantId + "@example.com").build() : null;
        Participant participant = withUser ? Participant.join(user, group, profile) : Participant.addByHost(group, profile);
        ReflectionTestUtils.setField(participant, "participantId", nextParticipantId++);
        return participant;
    }

    private GroupAssignment firstRoundAssignment() {
        GroupAssignment assignment = GroupAssignment.create(group, Round.FIRST_ROUND, 2, EnumSet.noneOf(AssignmentCondition.class));
        ReflectionTestUtils.setField(assignment, "assignmentId", 1L);
        return assignment;
    }

    private TeamAssignmentMember teamMember(GroupAssignment assignment, Participant participant, int teamNumber) {
        return TeamAssignmentMember.create(assignment, participant, teamNumber, false);
    }

    // ---------- voteMvp ----------

    @Test
    @DisplayName("같은 조원에게 MVP 투표를 하면 성공한다")
    void voteMvpSucceeds() {
        Participant voter = participant(true);
        Participant target = participant(true);
        MvpVoteReqDto dto = MvpVoteReqDto.builder().targetParticipantId(target.getParticipantId()).build();
        GroupAssignment assignment = firstRoundAssignment();

        when(groupMembership.getMember(1L, 1L)).thenReturn(voter);
        when(participantRepository.findByParticipantIdAndGroup(target.getParticipantId(), group))
                .thenReturn(Optional.of(target));
        when(groupAssignmentRepository.findByGroupAndRound(group, Round.FIRST_ROUND)).thenReturn(Optional.of(assignment));
        when(teamAssignmentMemberRepository.findByAssignmentAndParticipant(assignment, voter))
                .thenReturn(Optional.of(teamMember(assignment, voter, 1)));
        when(teamAssignmentMemberRepository.findByAssignmentAndParticipant(assignment, target))
                .thenReturn(Optional.of(teamMember(assignment, target, 1)));
        when(mvpVoteRepository.existsByVoter(voter)).thenReturn(false);

        voteService.voteMvp(dto, 1L, 1L);

        Mockito.verify(mvpVoteRepository).save(any());
    }

    @Test
    @DisplayName("자기 자신에게는 MVP 투표를 할 수 없다")
    void voteMvpFailsWhenVotingForSelf() {
        Participant voter = participant(true);
        MvpVoteReqDto dto = MvpVoteReqDto.builder().targetParticipantId(voter.getParticipantId()).build();

        when(groupMembership.getMember(1L, 1L)).thenReturn(voter);
        when(participantRepository.findByParticipantIdAndGroup(voter.getParticipantId(), group))
                .thenReturn(Optional.of(voter));

        assertThatThrownBy(() -> voteService.voteMvp(dto, 1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.VOTE_TARGET_NOT_IN_TEAM);
    }

    @Test
    @DisplayName("다른 조원에게는 MVP 투표를 할 수 없다")
    void voteMvpFailsWhenNotSameTeam() {
        Participant voter = participant(true);
        Participant target = participant(true);
        MvpVoteReqDto dto = MvpVoteReqDto.builder().targetParticipantId(target.getParticipantId()).build();
        GroupAssignment assignment = firstRoundAssignment();

        when(groupMembership.getMember(1L, 1L)).thenReturn(voter);
        when(participantRepository.findByParticipantIdAndGroup(target.getParticipantId(), group))
                .thenReturn(Optional.of(target));
        when(groupAssignmentRepository.findByGroupAndRound(group, Round.FIRST_ROUND)).thenReturn(Optional.of(assignment));
        when(teamAssignmentMemberRepository.findByAssignmentAndParticipant(assignment, voter))
                .thenReturn(Optional.of(teamMember(assignment, voter, 1)));
        when(teamAssignmentMemberRepository.findByAssignmentAndParticipant(assignment, target))
                .thenReturn(Optional.of(teamMember(assignment, target, 2)));

        assertThatThrownBy(() -> voteService.voteMvp(dto, 1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.VOTE_TARGET_NOT_IN_TEAM);
    }

    @Test
    @DisplayName("이미 MVP 투표를 했으면 다시 투표할 수 없다")
    void voteMvpFailsWhenAlreadyVoted() {
        Participant voter = participant(true);
        Participant target = participant(true);
        MvpVoteReqDto dto = MvpVoteReqDto.builder().targetParticipantId(target.getParticipantId()).build();
        GroupAssignment assignment = firstRoundAssignment();

        when(groupMembership.getMember(1L, 1L)).thenReturn(voter);
        when(participantRepository.findByParticipantIdAndGroup(target.getParticipantId(), group))
                .thenReturn(Optional.of(target));
        when(groupAssignmentRepository.findByGroupAndRound(group, Round.FIRST_ROUND)).thenReturn(Optional.of(assignment));
        when(teamAssignmentMemberRepository.findByAssignmentAndParticipant(assignment, voter))
                .thenReturn(Optional.of(teamMember(assignment, voter, 1)));
        when(teamAssignmentMemberRepository.findByAssignmentAndParticipant(assignment, target))
                .thenReturn(Optional.of(teamMember(assignment, target, 1)));
        when(mvpVoteRepository.existsByVoter(voter)).thenReturn(true);

        assertThatThrownBy(() -> voteService.voteMvp(dto, 1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_VOTED);
    }

    @Test
    @DisplayName("투표 진행중이 아니면 MVP 투표를 할 수 없다")
    void voteMvpFailsWhenNotVoting() {
        group.finishVoting(); // VOTE_CLOSED
        Participant voter = participant(true);
        MvpVoteReqDto dto = MvpVoteReqDto.builder().targetParticipantId(99L).build();

        when(groupMembership.getMember(1L, 1L)).thenReturn(voter);

        assertThatThrownBy(() -> voteService.voteMvp(dto, 1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.VOTE_NOT_IN_PROGRESS);
    }

    // ---------- voteSecondRound / updateSecondRoundVote ----------

    @Test
    @DisplayName("2차 참여로 투표하면 참가자의 2차 참여 상태도 함께 갱신된다")
    void voteSecondRoundSucceedsAndJoinsSecondRound() {
        Participant voter = participant(true);
        Round2VoteReqDto dto = Round2VoteReqDto.builder().choice(VoteChoice.PARTICIPATE).build();

        when(groupMembership.getMember(1L, 1L)).thenReturn(voter);
        when(round2VoteRepository.existsByVoter(voter)).thenReturn(false);

        voteService.voteSecondRound(dto, 1L, 1L);

        Mockito.verify(round2VoteRepository).save(any());
        assertThat(voter.getRoundParticipation().name()).isEqualTo("FIRST_AND_SECOND");
    }

    @Test
    @DisplayName("이미 투표했으면 2차 참여 투표를 다시 할 수 없다")
    void voteSecondRoundFailsWhenAlreadyVoted() {
        Participant voter = participant(true);
        Round2VoteReqDto dto = Round2VoteReqDto.builder().choice(VoteChoice.PARTICIPATE).build();

        when(groupMembership.getMember(1L, 1L)).thenReturn(voter);
        when(round2VoteRepository.existsByVoter(voter)).thenReturn(true);

        assertThatThrownBy(() -> voteService.voteSecondRound(dto, 1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_VOTED);
    }

    @Test
    @DisplayName("본인의 2차 참여 투표를 불참으로 정정하면 2차 참여 상태도 함께 내려간다")
    void updateSecondRoundVoteSucceeds() {
        Participant voter = participant(true);
        voter.joinSecondRound();
        Round2VoteReqDto dto = Round2VoteReqDto.builder().choice(VoteChoice.NOT_PARTICIPATE).build();
        Round2ParticipationVote existingVote = Round2ParticipationVote.create(voter, VoteChoice.PARTICIPATE);

        when(groupMembership.getMember(1L, 1L)).thenReturn(voter);
        when(round2VoteRepository.findByVoter(voter)).thenReturn(Optional.of(existingVote));

        voteService.updateSecondRoundVote(dto, 1L, 1L);

        assertThat(existingVote.getChoice()).isEqualTo(VoteChoice.NOT_PARTICIPATE);
        assertThat(voter.getRoundParticipation().name()).isEqualTo("FIRST_ONLY");
    }

    @Test
    @DisplayName("투표 기록이 없으면 정정할 수 없다")
    void updateSecondRoundVoteFailsWhenNoExistingVote() {
        Participant voter = participant(true);
        Round2VoteReqDto dto = Round2VoteReqDto.builder().choice(VoteChoice.NOT_PARTICIPATE).build();

        when(groupMembership.getMember(1L, 1L)).thenReturn(voter);
        when(round2VoteRepository.findByVoter(voter)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voteService.updateSecondRoundVote(dto, 1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    // ---------- voteSecondRoundByHost / updateSecondRoundVoteByHost ----------

    @Test
    @DisplayName("관리자가 대리 등록 참가자를 대신해 2차 참여 투표를 할 수 있다")
    void voteSecondRoundByHostSucceeds() {
        Participant host = participant(true);
        Participant manual = participant(false); // addByHost: user == null
        AdminRound2VoteReqDto dto = AdminRound2VoteReqDto.builder()
                .targetParticipantId(manual.getParticipantId()).choice(VoteChoice.PARTICIPATE).build();

        when(groupMembership.getHost(1L, 1L)).thenReturn(host);
        when(participantRepository.findByParticipantIdAndGroup(manual.getParticipantId(), group))
                .thenReturn(Optional.of(manual));
        when(round2VoteRepository.existsByVoter(manual)).thenReturn(false);

        voteService.voteSecondRoundByHost(dto, 1L, 1L);

        Mockito.verify(round2VoteRepository).save(any());
        assertThat(manual.getRoundParticipation().name()).isEqualTo("FIRST_AND_SECOND");
    }

    @Test
    @DisplayName("로그인 계정이 있는 참가자는 관리자가 대신 투표할 수 없다")
    void voteSecondRoundByHostFailsForAccountParticipant() {
        Participant host = participant(true);
        Participant target = participant(true); // join: has a real user
        AdminRound2VoteReqDto dto = AdminRound2VoteReqDto.builder()
                .targetParticipantId(target.getParticipantId()).choice(VoteChoice.PARTICIPATE).build();

        when(groupMembership.getHost(1L, 1L)).thenReturn(host);
        when(participantRepository.findByParticipantIdAndGroup(target.getParticipantId(), group))
                .thenReturn(Optional.of(target));

        assertThatThrownBy(() -> voteService.voteSecondRoundByHost(dto, 1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_MANUAL_PARTICIPANT);
    }

    @Test
    @DisplayName("관리자가 대리 등록 참가자의 2차 참여 투표를 정정할 수 있다")
    void updateSecondRoundVoteByHostSucceeds() {
        Participant host = participant(true);
        Participant manual = participant(false);
        manual.joinSecondRound();
        AdminRound2VoteReqDto dto = AdminRound2VoteReqDto.builder()
                .targetParticipantId(manual.getParticipantId()).choice(VoteChoice.NOT_PARTICIPATE).build();
        Round2ParticipationVote existingVote = Round2ParticipationVote.create(manual, VoteChoice.PARTICIPATE);

        when(groupMembership.getHost(1L, 1L)).thenReturn(host);
        when(participantRepository.findByParticipantIdAndGroup(manual.getParticipantId(), group))
                .thenReturn(Optional.of(manual));
        when(round2VoteRepository.findByVoter(manual)).thenReturn(Optional.of(existingVote));

        voteService.updateSecondRoundVoteByHost(dto, 1L, 1L);

        assertThat(existingVote.getChoice()).isEqualTo(VoteChoice.NOT_PARTICIPATE);
        assertThat(manual.getRoundParticipation().name()).isEqualTo("FIRST_ONLY");
    }

    // ---------- getSecondRoundVoteStatus ----------

    @Test
    @DisplayName("2차 참여 투표 현황을 참여/불참/미투표로 집계한다")
    void getSecondRoundVoteStatusSucceeds() {
        Participant me = participant(true);
        Participant voted = participant(true);
        Participant notVoted = participant(true);
        Round2ParticipationVote vote = Round2ParticipationVote.create(voted, VoteChoice.PARTICIPATE);

        when(groupMembership.getMember(1L, 1L)).thenReturn(me);
        when(participantRepository.findByGroup(group)).thenReturn(List.of(me, voted, notVoted));
        when(round2VoteRepository.findByVoter_Group(group)).thenReturn(List.of(vote));

        Round2VoteStatusResDto result = voteService.getSecondRoundVoteStatus(1L, 1L);

        assertThat(result.getTotalParticipantCount()).isEqualTo(3);
        assertThat(result.getVotedCount()).isEqualTo(1);
        assertThat(result.getParticipateCount()).isEqualTo(1);
        assertThat(result.getNotParticipateCount()).isEqualTo(0);
    }

    // ---------- getVoteResult ----------

    @Test
    @DisplayName("투표가 아직 끝나지 않았으면 결과를 조회할 수 없다")
    void getVoteResultFailsWhenVoteNotFinished() {
        // group은 setUp에서 VOTING 상태 — 아직 종료 전
        Participant me = participant(true);
        when(groupMembership.getMember(1L, 1L)).thenReturn(me);

        assertThatThrownBy(() -> voteService.getVoteResult(1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_GROUP_STATUS);
    }

    @Test
    @DisplayName("투표 종료 후에는 최다 득표자를 MVP로 조회한다")
    void getVoteResultSucceeds() {
        group.finishVoting(); // VOTE_CLOSED -> isVoteFinished() == true
        Participant me = participant(true);
        Participant winner = participant(true);

        when(groupMembership.getMember(1L, 1L)).thenReturn(me);
        when(mvpVoteRepository.findByTarget_Group(group))
                .thenReturn(List.of(com.mixmate.domain.vote.entity.MvpVote.create(me, winner)));
        when(groupAssignmentRepository.findByGroupAndRound(group, Round.FIRST_ROUND)).thenReturn(Optional.empty());
        when(participantRepository.findByGroupAndRoundParticipation(group,
                com.mixmate.domain.participant.enums.RoundParticipation.FIRST_AND_SECOND)).thenReturn(List.of());

        VoteResultResDto result = voteService.getVoteResult(1L, 1L);

        assertThat(result.getMvpWinners()).hasSize(1);
        assertThat(result.getMvpWinners().get(0).getParticipantId()).isEqualTo(winner.getParticipantId());
    }

    // ---------- finishVote ----------

    @Test
    @DisplayName("관리자가 투표를 강제 종료하면 미투표자는 전부 2차 불참으로 자동 처리된다")
    void finishVoteSucceeds() {
        Participant host = participant(true);
        Participant voted = participant(true);
        Participant notVoted = participant(true);
        Round2ParticipationVote vote = Round2ParticipationVote.create(voted, VoteChoice.PARTICIPATE);

        when(groupMembership.getHost(1L, 1L)).thenReturn(host);
        when(participantRepository.findByGroup(group)).thenReturn(List.of(host, voted, notVoted));
        when(round2VoteRepository.findByVoter_Group(group)).thenReturn(List.of(vote));

        voteService.finishVote(1L, 1L);

        Mockito.verify(round2VoteRepository).saveAll(Mockito.argThat(list -> {
            List<?> l = (List<?>) list;
            return l.size() == 2; // host, notVoted 두 명이 자동으로 불참 처리됨
        }));
        assertThat(group.getStatus()).isEqualTo(com.mixmate.domain.group.enums.GroupStatus.VOTE_CLOSED);
        Mockito.verify(eventPublisher).publishEvent(any(GroupStatusChangedEvent.class));
    }

    @Test
    @DisplayName("투표 진행중이 아니면 강제 종료할 수 없다")
    void finishVoteFailsWhenNotVoting() {
        group.finishVoting(); // 이미 VOTE_CLOSED
        Participant host = participant(true);
        when(groupMembership.getHost(1L, 1L)).thenReturn(host);

        assertThatThrownBy(() -> voteService.finishVote(1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.VOTE_NOT_IN_PROGRESS);
    }

    @Test
    @DisplayName("투표 종료 시 MVP 최다 득표자의 Participant가 isMvp로 표시된다")
    void finishVoteMarksWinnerAsMvp() {
        Participant host = participant(true);
        Participant winner = participant(true);
        Participant loser = participant(true);
        com.mixmate.domain.vote.entity.MvpVote voteForWinner1 =
                com.mixmate.domain.vote.entity.MvpVote.create(host, winner);
        com.mixmate.domain.vote.entity.MvpVote voteForWinner2 =
                com.mixmate.domain.vote.entity.MvpVote.create(loser, winner);
        com.mixmate.domain.vote.entity.MvpVote voteForLoser =
                com.mixmate.domain.vote.entity.MvpVote.create(winner, loser);

        when(groupMembership.getHost(1L, 1L)).thenReturn(host);
        when(participantRepository.findByGroup(group)).thenReturn(List.of(host, winner, loser));
        when(round2VoteRepository.findByVoter_Group(group)).thenReturn(List.of());
        when(mvpVoteRepository.findByTarget_Group(group))
                .thenReturn(List.of(voteForWinner1, voteForWinner2, voteForLoser));

        voteService.finishVote(1L, 1L);

        assertThat(winner.isMvp()).isTrue();
        assertThat(loser.isMvp()).isFalse();
    }

    @Test
    @DisplayName("로그인 계정이 없는 대리 등록 참가자도 MVP로 뽑히면 Participant에는 정상적으로 표시된다")
    void finishVoteMarksManualParticipantAsMvpToo() {
        Participant host = participant(true);
        Participant manualWinner = participant(false); // addByHost: user == null
        com.mixmate.domain.vote.entity.MvpVote vote =
                com.mixmate.domain.vote.entity.MvpVote.create(host, manualWinner);

        when(groupMembership.getHost(1L, 1L)).thenReturn(host);
        when(participantRepository.findByGroup(group)).thenReturn(List.of(host, manualWinner));
        when(round2VoteRepository.findByVoter_Group(group)).thenReturn(List.of());
        when(mvpVoteRepository.findByTarget_Group(group)).thenReturn(List.of(vote));

        voteService.finishVote(1L, 1L);

        // User가 없어도 isMvp는 Participant 자체에 남는 값이라 문제없이 표시된다
        // (계정 전체 집계에는 안 잡히지만, 이 모임 안에서 MVP였다는 사실 자체는 정상적으로 기록됨)
        assertThat(manualWinner.isMvp()).isTrue();
        assertThat(group.getStatus()).isEqualTo(com.mixmate.domain.group.enums.GroupStatus.VOTE_CLOSED);
    }
}
