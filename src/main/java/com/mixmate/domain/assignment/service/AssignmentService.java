package com.mixmate.domain.assignment.service;

import com.mixmate.domain.assignment.dto.FixedMember;
import com.mixmate.domain.assignment.dto.TeamDetail;
import com.mixmate.domain.assignment.dto.request.TeamGenerateRequest;
import com.mixmate.domain.assignment.dto.response.MyTeamResponse;
import com.mixmate.domain.assignment.dto.response.TeamAssignmentResponse;
import com.mixmate.domain.assignment.entity.GroupAssignment;
import com.mixmate.domain.assignment.entity.TeamAssignmentMember;
import com.mixmate.domain.assignment.repository.GroupAssignmentRepository;
import com.mixmate.domain.assignment.repository.TeamAssignmentMemberRepository;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.enums.Role;
import com.mixmate.domain.participant.enums.Round;
import com.mixmate.domain.participant.enums.RoundParticipation;
import com.mixmate.domain.participant.repository.ParticipantRepository;
import com.mixmate.domain.participant.service.GroupMembership;
import com.mixmate.exception.CustomException;
import com.mixmate.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 라운드별 조 편성의 실행과 확정을 관리자(HOST) 권한 기준으로 처리하는 서비스입니다.
 *
 * 배치를 만드는 것과 확정하는 것이 나뉘어 있습니다. 실행은 결과를 저장만 하고,
 * 확정이 그룹 상태를 다음 라운드로 넘깁니다. 확정 여부를 따로 저장하지 않고 그룹 상태로 판단합니다.
 *
 * 실제 배치 계산은 TeamAssigner가 담당하며, 이 클래스는 검증과 저장만 맡습니다.
 */
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final GroupAssignmentRepository groupAssignmentRepository;
    private final TeamAssignmentMemberRepository teamAssignmentMemberRepository;
    private final ParticipantRepository participantRepository;
    private final GroupMembership groupMembership;
    private final TeamAssigner teamAssigner;

    /**
     * 관리자가 조 편성을 실행합니다. 1차는 참가자 전원이, 2차는 2차 참여를 선택한 인원만 대상입니다.
     * 확정 전이면 몇 번이든 다시 실행할 수 있고, 그때마다 기존 편성 결과를 덮어씁니다.
     */
    @Transactional
    public TeamAssignmentResponse generate(TeamGenerateRequest dto, Round round, Long groupId, Long userId) {
        Group group = groupMembership.getHost(groupId, userId).getGroup();

        if (!group.getStatus().canAssign(round)) {
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS, "조 편성 대기 중일 때만 편성할 수 있습니다.");
        }

        // 2차는 참여하겠다고 투표한 인원만 대상이다
        List<Participant> participants = (round == Round.FIRST_ROUND)
                ? participantRepository.findByGroup(group)
                : participantRepository.findByGroupAndRoundParticipation(group, RoundParticipation.FIRST_AND_SECOND);

        if (participants.size() < dto.teamCount()) {
            throw new CustomException(ErrorCode.INSUFFICIENT_PARTICIPANTS);
        }

        Map<Long, Integer> fixedMembers = toFixedMembers(dto, participants);

        // 라운드당 배치는 하나뿐이라 재실행은 새로 만들지 않고 기존 배치를 갱신한다.
        // 같은 참가자가 다시 삽입되므로 기존 조원을 먼저 지운다(벌크 삭제라 즉시 DB에 반영된다).
        // 조원이 배치를 참조하므로 새 배치는 먼저 저장해 식별자를 받는다.
        GroupAssignment assignment = groupAssignmentRepository.findByGroupAndRound(group, round)
                .map(existing -> {
                    teamAssignmentMemberRepository.deleteAllByAssignment(existing);
                    existing.regenerate(dto.teamCount(), dto.conditions());
                    return existing;
                })
                .orElseGet(() -> groupAssignmentRepository.save(
                        GroupAssignment.create(group, round, dto.teamCount(), dto.conditions())));

        Map<Integer, List<Participant>> teams =
                teamAssigner.assign(participants, dto.teamCount(), dto.conditions(), fixedMembers);

        // 계산 결과(참가자)를 저장할 엔티티로 옮긴다. 고정 여부는 요청에 실려온 목록으로 판단한다.
        Map<Integer, List<TeamAssignmentMember>> assignedTeams = new LinkedHashMap<>();
        teams.forEach((teamNumber, members) -> assignedTeams.put(teamNumber, members.stream()
                .map(participant -> TeamAssignmentMember.create(assignment, participant, teamNumber,
                        fixedMembers.containsKey(participant.getParticipantId())))
                .toList()));

        teamAssignmentMemberRepository.saveAll(
                assignedTeams.values().stream().flatMap(List::stream).toList());

        List<TeamDetail> teamDetails = assignedTeams.entrySet().stream()
                .map(entry -> TeamDetail.of(entry.getKey(), entry.getValue()))
                .toList();

        return new TeamAssignmentResponse(round, dto.teamCount(), dto.conditions(), teamDetails);
    }

    /**
     * 관리자가 편성 결과를 확정해 해당 라운드를 시작합니다. 되돌릴 수 없습니다.
     * 이미 그 라운드가 시작된 그룹에 다시 호출하면 아무 일도 하지 않고 성공으로 응답합니다.
     */
    @Transactional
    public void confirm(Round round, Long groupId, Long userId) {
        Group group = groupMembership.getHost(groupId, userId).getGroup();

        // 요청한 라운드가 이미 시작됐을 때만 멱등 처리한다.
        if (group.getStatus().isRoundInProgress(round)) return;

        if (!group.getStatus().canAssign(round)) {
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS, "조 편성 대기 중일 때만 확정할 수 있습니다.");
        }
        // 상태는 맞지만 편성을 건너뛴 경우
        if (groupAssignmentRepository.findByGroupAndRound(group, round).isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS, "조 편성을 먼저 실행해야 합니다.");
        }

        group.startRound(round);
    }

    /**
     * 해당 차수의 조 편성 전체를 조회합니다. 편성이 확정된 뒤에만 볼 수 있습니다.
     */
    @Transactional(readOnly = true)
    public TeamAssignmentResponse getTeams(Round round, Long groupId, Long userId) {
        Participant me = groupMembership.getMember(groupId, userId);
        Group group = me.getGroup();

        if (!group.getStatus().isAssignmentConfirmed(round)) {
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS, "조 편성이 확정된 뒤에 조회할 수 있습니다.");
        }

        GroupAssignment assignment = groupAssignmentRepository.findByGroupAndRound(group, round)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_GROUP_STATUS, "해당 차수의 조 편성이 없습니다."));

        // 그 차수에 배정된 사람만 조 편성을 볼 수 있다. 관리자는 참여하지 않더라도 진행을 맡으므로 예외다.
        if (me.getRole() != Role.HOST
                && teamAssignmentMemberRepository.findByAssignmentAndParticipant(assignment, me).isEmpty()) {
            throw new CustomException(ErrorCode.FORBIDDEN, "2차 참여를 선택한 참가자만 조회할 수 있습니다.");
        }

        // 조 번호 순으로 정렬해 가져온 뒤 조 단위로 묶는다. LinkedHashMap이라 정렬이 유지된다.
        Map<Integer, List<TeamAssignmentMember>> membersByTeam =
                teamAssignmentMemberRepository.findByAssignment(assignment).stream()
                        .collect(Collectors.groupingBy(TeamAssignmentMember::getTeamNumber,
                                LinkedHashMap::new, Collectors.toList()));

        List<TeamDetail> teams = membersByTeam.entrySet().stream()
                .map(entry -> TeamDetail.of(entry.getKey(), entry.getValue()))
                .toList();

        // 엔티티의 지연 컬렉션을 그대로 내보내면 트랜잭션이 끝난 뒤 직렬화되면서 초기화에 실패한다
        return new TeamAssignmentResponse(round, assignment.getTeamCount(),
                Set.copyOf(assignment.getConditions()), teams);
    }

    /**
     * 참가자가 해당 차수에서 자신이 배정된 조와 조원을 조회합니다.
     * 편성이 확정되기 전에는 볼 수 없고, 그 차수의 편성 대상이 아니었던 참가자도 볼 수 없습니다.
     */
    @Transactional(readOnly = true)
    public MyTeamResponse getMyTeam(Round round, Long groupId, Long userId) {
        Participant me = groupMembership.getMember(groupId, userId);
        Group group = me.getGroup();

        // 확정 전 결과는 관리자가 재셔플하는 중일 수 있어 참가자에게 열지 않는다
        if (!group.getStatus().isAssignmentConfirmed(round)) {
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS, "조 편성이 확정된 뒤에 조회할 수 있습니다.");
        }

        GroupAssignment assignment = groupAssignmentRepository.findByGroupAndRound(group, round)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_GROUP_STATUS, "해당 차수의 조 편성이 없습니다."));

        // 입장은 모집 중에만 가능하므로 계정이 있는 참가자는 1차 배치에 반드시 들어 있다.
        // 즉 여기서 걸리는 것은 2차 참여를 선택하지 않아 2차 배치에서 빠진 참가자뿐이다.
        TeamAssignmentMember myAssignment = teamAssignmentMemberRepository.findByAssignmentAndParticipant(assignment, me)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN, "2차 참여를 선택한 참가자만 조회할 수 있습니다."));

        List<TeamAssignmentMember> teammates =
                teamAssignmentMemberRepository.findByAssignmentAndTeamNumber(assignment, myAssignment.getTeamNumber());

        return new MyTeamResponse(round, TeamDetail.of(myAssignment.getTeamNumber(), teammates));
    }

    /**
     * 요청으로 들어온 고정 멤버를 참가자 id → 조 번호 맵으로 바꾸면서 함께 검증한다.
     * 클라이언트가 보낸 값이므로 이 그룹 참가자인지, 조 번호가 실재하는지, 중복 지정은 없는지 여기서 걸러낸다.
     */
    private Map<Long, Integer> toFixedMembers(TeamGenerateRequest dto, List<Participant> participants) {
        Set<Long> participantIds = participants.stream()
                .map(Participant::getParticipantId)
                .collect(Collectors.toSet());

        Map<Long, Integer> fixedMembers = new HashMap<>();
        for (FixedMember fixedMember : dto.fixedMembers()) {
            if (!participantIds.contains(fixedMember.participantId())) {
                throw new CustomException(ErrorCode.INVALID_PARAMETER, "조 편성 대상이 아닌 참가자는 고정할 수 없습니다.");
            }
            if (fixedMember.teamNumber() > dto.teamCount()) {
                throw new CustomException(ErrorCode.INVALID_PARAMETER, "조 번호가 조 개수를 벗어났습니다.");
            }
            if (fixedMembers.put(fixedMember.participantId(), fixedMember.teamNumber()) != null) {
                throw new CustomException(ErrorCode.INVALID_PARAMETER, "같은 참가자를 두 번 고정할 수 없습니다.");
            }
        }
        return fixedMembers;
    }
}
