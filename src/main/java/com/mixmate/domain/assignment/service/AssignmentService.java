package com.mixmate.domain.assignment.service;

import com.mixmate.domain.assignment.dto.FixedMember;
import com.mixmate.domain.assignment.dto.TeamDetail;
import com.mixmate.domain.assignment.dto.request.TeamGenerateRequest;
import com.mixmate.domain.assignment.dto.response.TeamAssignmentResponse;
import com.mixmate.domain.assignment.entity.GroupAssignment;
import com.mixmate.domain.assignment.entity.TeamAssignmentMember;
import com.mixmate.domain.assignment.repository.GroupAssignmentRepository;
import com.mixmate.domain.assignment.repository.TeamAssignmentMemberRepository;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.participant.entity.Participant;
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
     * 재실행(재셔플)은 아직 없어 같은 라운드에 두 번 호출할 수 없습니다.
     */
    @Transactional
    public TeamAssignmentResponse generate(TeamGenerateRequest dto, Round round, Long groupId, Long userId) {
        Group group = groupMembership.getHost(groupId, userId).getGroup();

        if (!group.getStatus().canAssign(round)) {
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS, "조 편성 대기 중일 때만 편성할 수 있습니다.");
        }
        // 재실행(재셔플)은 아직 없다. 같은 라운드의 배치는 하나뿐이라 두 번째 요청은 막아뒀다
        if (groupAssignmentRepository.findByGroupAndRound(group, round).isPresent()) {
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS, "이미 조 편성을 실행했습니다.");
        }

        // 2차는 참여하겠다고 투표한 인원만 대상이다
        List<Participant> participants = (round == Round.FIRST_ROUND)
                ? participantRepository.findByGroup(group)
                : participantRepository.findByGroupAndRoundParticipation(group, RoundParticipation.FIRST_AND_SECOND);

        if (participants.size() < dto.teamCount()) {
            throw new CustomException(ErrorCode.INSUFFICIENT_PARTICIPANTS);
        }

        Map<Long, Integer> fixedMembers = toFixedMembers(dto, participants);

        // 조원이 배치를 참조하므로 배치를 먼저 저장해 식별자를 받는다
        GroupAssignment assignment = groupAssignmentRepository.save(
                GroupAssignment.create(group, round, dto.teamCount(), dto.conditions()));

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
