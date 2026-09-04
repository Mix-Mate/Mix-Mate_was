package com.mixmate.domain.vote.service;

import com.mixmate.domain.assignment.entity.GroupAssignment;
import com.mixmate.domain.assignment.entity.TeamAssignmentMember;
import com.mixmate.domain.assignment.repository.GroupAssignmentRepository;
import com.mixmate.domain.assignment.repository.TeamAssignmentMemberRepository;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.group.enums.GroupStatus;
import com.mixmate.domain.group.event.GroupStatusChangedEvent;
import com.mixmate.domain.participant.dto.ParticipantSummary;
import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.enums.Round;
import com.mixmate.domain.participant.enums.RoundParticipation;
import com.mixmate.domain.participant.repository.ParticipantRepository;
import com.mixmate.domain.participant.service.GroupMembership;
import com.mixmate.domain.vote.dto.request.AdminRound2VoteReqDto;
import com.mixmate.domain.vote.dto.request.MvpVoteReqDto;
import com.mixmate.domain.vote.dto.request.Round2VoteReqDto;
import com.mixmate.domain.vote.dto.response.MvpWinnerResDto;
import com.mixmate.domain.vote.dto.response.ParticipantVoteStatusResDto;
import com.mixmate.domain.vote.dto.response.Round2VoteStatusResDto;
import com.mixmate.domain.vote.dto.response.VoteResultResDto;
import com.mixmate.domain.vote.entity.MvpVote;
import com.mixmate.domain.vote.entity.Round2ParticipationVote;
import com.mixmate.domain.vote.enums.VoteChoice;
import com.mixmate.domain.vote.repository.MvpVoteRepository;
import com.mixmate.domain.vote.repository.Round2ParticipationVoteRepository;
import com.mixmate.exception.CustomException;
import com.mixmate.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MVP 투표, 2차 참여 여부 투표, 그리고 그 현황/결과 조회와 관리자의 강제 종료를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class VoteService {

    private final ParticipantRepository participantRepository;
    private final MvpVoteRepository mvpVoteRepository;
    private final Round2ParticipationVoteRepository round2VoteRepository;
    private final GroupAssignmentRepository groupAssignmentRepository;
    private final TeamAssignmentMemberRepository teamAssignmentMemberRepository;
    private final GroupMembership groupMembership;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 참가자가 같은 조원 중 한 명에게 MVP 투표를 한다.
     * 본인에게는 투표할 수 없고, 1차 조 편성 기준으로 같은 조원에게만 투표할 수 있으며, 한 번만 투표할 수 있다.
     */
    @Transactional
    public void voteMvp(MvpVoteReqDto dto, Long groupId, Long userId) {
        Participant voter = groupMembership.getMember(groupId, userId);
        Group group = voter.getGroup();

        if (group.getStatus() != GroupStatus.VOTING)
            throw new CustomException(ErrorCode.VOTE_NOT_IN_PROGRESS);

        Participant target = participantRepository.findByParticipantIdAndGroup(dto.getTargetParticipantId(), group)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "투표 대상을 찾을 수 없습니다."));

        if (target.getParticipantId().equals(voter.getParticipantId()))
            throw new CustomException(ErrorCode.VOTE_TARGET_NOT_IN_TEAM, "자신에게는 투표할 수 없습니다.");

        if (!sameFirstRoundTeam(group, voter, target))
            throw new CustomException(ErrorCode.VOTE_TARGET_NOT_IN_TEAM);

        if (mvpVoteRepository.existsByVoter(voter))
            throw new CustomException(ErrorCode.ALREADY_VOTED);

        mvpVoteRepository.save(MvpVote.create(voter, target));
    }

    private boolean sameFirstRoundTeam(Group group, Participant voter, Participant target) {
        GroupAssignment assignment = groupAssignmentRepository.findByGroupAndRound(group, Round.FIRST_ROUND)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_GROUP_STATUS, "조 편성이 완료되지 않았습니다."));

        int voterTeam = teamAssignmentMemberRepository.findByAssignmentAndParticipant(assignment, voter)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_GROUP_STATUS, "조 편성이 완료되지 않았습니다."))
                .getTeamNumber();
        int targetTeam = teamAssignmentMemberRepository.findByAssignmentAndParticipant(assignment, target)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_GROUP_STATUS, "조 편성이 완료되지 않았습니다."))
                .getTeamNumber();

        return voterTeam == targetTeam;
    }

    /**
     * 참가자가 2차 참여 여부를 투표한다. 참여를 선택하면 참가자의 라운드 참여 상태도 함께 갱신된다.
     * 한 번만 투표할 수 있다.
     */
    @Transactional
    public void voteSecondRound(Round2VoteReqDto dto, Long groupId, Long userId) {
        Participant voter = groupMembership.getMember(groupId, userId);
        Group group = voter.getGroup();

        if (group.getStatus() != GroupStatus.VOTING)
            throw new CustomException(ErrorCode.VOTE_NOT_IN_PROGRESS);
        if (round2VoteRepository.existsByVoter(voter))
            throw new CustomException(ErrorCode.ALREADY_VOTED);

        round2VoteRepository.save(Round2ParticipationVote.create(voter, dto.getChoice()));
        if (dto.getChoice() == VoteChoice.PARTICIPATE)
            voter.joinSecondRound();
    }

    /**
     * 참가자가 자신이 투표한 2차 참여 여부를 정정한다. voteSecondRound와 달리 이미 투표된 건을 대상으로 한다.
     */
    @Transactional
    public void updateSecondRoundVote(Round2VoteReqDto dto, Long groupId, Long userId) {
        Participant voter = groupMembership.getMember(groupId, userId);
        Group group = voter.getGroup();

        if (group.getStatus() != GroupStatus.VOTING)
            throw new CustomException(ErrorCode.VOTE_NOT_IN_PROGRESS);

        Round2ParticipationVote vote = round2VoteRepository.findByVoter(voter)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "투표 기록이 없습니다."));

        vote.updateChoice(dto.getChoice());
        if (dto.getChoice() == VoteChoice.PARTICIPATE)
            voter.joinSecondRound();
        else
            voter.leaveSecondRound();
    }

    /**
     * 관리자가 로그인 계정이 없는(대리 등록한) 참가자를 대신해 2차 참여 여부를 투표한다.
     * 계정이 있는 참가자는 본인만 투표할 수 있으므로 이 경로로 대신 투표할 수 없다.
     */
    @Transactional
    public void voteSecondRoundByHost(AdminRound2VoteReqDto dto, Long groupId, Long userId) {
        Group group = groupMembership.getHost(groupId, userId).getGroup();

        if (group.getStatus() != GroupStatus.VOTING)
            throw new CustomException(ErrorCode.VOTE_NOT_IN_PROGRESS);

        Participant target = participantRepository.findByParticipantIdAndGroup(dto.getTargetParticipantId(), group)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "투표 대상을 찾을 수 없습니다."));

        if (target.getUser() != null)
            throw new CustomException(ErrorCode.NOT_MANUAL_PARTICIPANT);

        if (round2VoteRepository.existsByVoter(target))
            throw new CustomException(ErrorCode.ALREADY_VOTED);

        round2VoteRepository.save(Round2ParticipationVote.create(target, dto.getChoice()));
        if (dto.getChoice() == VoteChoice.PARTICIPATE)
            target.joinSecondRound();
    }

    /**
     * 관리자가 대신 투표해준 2차 참여 여부를 정정한다. voteSecondRoundByHost와 달리 이미 투표된 건을
     * 대상으로 하며, 계정이 없는(대리 등록한) 참가자만 대상이다.
     */
    @Transactional
    public void updateSecondRoundVoteByHost(AdminRound2VoteReqDto dto, Long groupId, Long userId) {
        Group group = groupMembership.getHost(groupId, userId).getGroup();

        if (group.getStatus() != GroupStatus.VOTING)
            throw new CustomException(ErrorCode.VOTE_NOT_IN_PROGRESS);

        Participant target = participantRepository.findByParticipantIdAndGroup(dto.getTargetParticipantId(), group)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "투표 대상을 찾을 수 없습니다."));

        if (target.getUser() != null)
            throw new CustomException(ErrorCode.NOT_MANUAL_PARTICIPANT);

        Round2ParticipationVote vote = round2VoteRepository.findByVoter(target)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "투표 기록이 없습니다."));

        vote.updateChoice(dto.getChoice());
        if (dto.getChoice() == VoteChoice.PARTICIPATE)
            target.joinSecondRound();
        else
            target.leaveSecondRound();
    }

    /**
     * 2차 참여 여부 투표 현황(완료/참여/불참/미투표 인원과 참가자별 상태)을 조회한다.
     */
    @Transactional(readOnly = true)
    public Round2VoteStatusResDto getSecondRoundVoteStatus(Long groupId, Long userId) {
        Participant me = groupMembership.getMember(groupId, userId);
        Group group = me.getGroup();

        List<Participant> participants = participantRepository.findByGroup(group);
        Map<Long, VoteChoice> votedChoices = round2VoteRepository.findByVoter_Group(group).stream()
                .collect(Collectors.toMap(v -> v.getVoter().getParticipantId(), Round2ParticipationVote::getChoice));

        List<ParticipantVoteStatusResDto> statuses = participants.stream()
                .map(p -> ParticipantVoteStatusResDto.fromEntity(p, votedChoices.get(p.getParticipantId())))
                .toList();

        int participateCount = (int) votedChoices.values().stream().filter(c -> c == VoteChoice.PARTICIPATE).count();
        int notParticipateCount = (int) votedChoices.values().stream().filter(c -> c == VoteChoice.NOT_PARTICIPATE).count();

        return Round2VoteStatusResDto.builder()
                .totalParticipantCount(participants.size())
                .votedCount(votedChoices.size())
                .participateCount(participateCount)
                .notParticipateCount(notParticipateCount)
                .participants(statuses)
                .build();
    }

    /**
     * MVP 결과(동점자가 있으면 공동 MVP로 모두 포함)와 2차 참여자 명단을 조회한다.
     * 투표가 진행중이거나 시작 전이면 조회할 수 없다.
     */
    @Transactional(readOnly = true)
    public VoteResultResDto getVoteResult(Long groupId, Long userId) {
        Participant me = groupMembership.getMember(groupId, userId);
        Group group = me.getGroup();

        if (!group.getStatus().isVoteFinished())
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS, "투표가 아직 종료되지 않았습니다.");

        Map<Long, List<MvpVote>> votesByTargetId = mvpVoteRepository.findByTarget_Group(group).stream()
                .collect(Collectors.groupingBy(v -> v.getTarget().getParticipantId()));
        int maxVoteCount = votesByTargetId.values().stream().mapToInt(List::size).max().orElse(0);

        GroupAssignment firstRoundAssignment = groupAssignmentRepository.findByGroupAndRound(group, Round.FIRST_ROUND)
                .orElse(null);

        List<MvpWinnerResDto> mvpWinners = votesByTargetId.values().stream()
                .filter(votes -> maxVoteCount > 0 && votes.size() == maxVoteCount)
                .map(votes -> {
                    Participant target = votes.get(0).getTarget();
                    Integer teamNumber = (firstRoundAssignment == null) ? null
                            : teamAssignmentMemberRepository.findByAssignmentAndParticipant(firstRoundAssignment, target)
                                    .map(TeamAssignmentMember::getTeamNumber)
                                    .orElse(null);
                    return MvpWinnerResDto.fromEntity(target, teamNumber);
                })
                .toList();

        List<ParticipantSummary> secondRoundParticipants = participantRepository
                .findByGroupAndRoundParticipation(group, RoundParticipation.FIRST_AND_SECOND).stream()
                .map(ParticipantSummary::from)
                .toList();

        return VoteResultResDto.builder()
                .mvpWinners(mvpWinners)
                .secondRoundParticipants(secondRoundParticipants)
                .build();
    }

    /**
     * 관리자가 투표를 강제 종료한다. 미투표자는 모두 2차 불참으로 자동 처리되고, 그룹은 조 편성 대기 상태로 넘어간다.
     */
    @Transactional
    public void finishVote(Long groupId, Long userId) {
        Group group = groupMembership.getHost(groupId, userId).getGroup();

        if (group.getStatus() != GroupStatus.VOTING)
            throw new CustomException(ErrorCode.VOTE_NOT_IN_PROGRESS);

        List<Participant> participants = participantRepository.findByGroup(group);
        Set<Long> votedVoterIds = round2VoteRepository.findByVoter_Group(group).stream()
                .map(v -> v.getVoter().getParticipantId())
                .collect(Collectors.toSet());

        List<Round2ParticipationVote> autoVotes = participants.stream()
                .filter(p -> !votedVoterIds.contains(p.getParticipantId()))
                .map(p -> Round2ParticipationVote.create(p, VoteChoice.NOT_PARTICIPATE))
                .toList();
        round2VoteRepository.saveAll(autoVotes);

        group.finishVoting();
        eventPublisher.publishEvent(new GroupStatusChangedEvent(groupId, group.getStatus()));
    }
}
