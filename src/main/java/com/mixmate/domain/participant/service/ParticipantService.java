package com.mixmate.domain.participant.service;

import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.group.entity.GroupBan;
import com.mixmate.domain.group.enums.GroupStatus;
import com.mixmate.domain.group.repository.GroupBanRepository;
import com.mixmate.domain.group.repository.GroupRepository;
import com.mixmate.domain.participant.dto.request.ParticipantProfileRequest;
import com.mixmate.domain.participant.dto.response.MyProfileResponse;
import com.mixmate.domain.participant.dto.response.ParticipantListResponse;
import com.mixmate.domain.participant.dto.response.ParticipantProfileResponse;
import com.mixmate.domain.participant.dto.ParticipantSummary;
import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.enums.Role;
import com.mixmate.domain.participant.enums.Round;
import com.mixmate.domain.participant.enums.RoundParticipation;
import com.mixmate.domain.participant.enums.Visibility;
import com.mixmate.domain.participant.repository.ParticipantRepository;
import com.mixmate.exception.CustomException;
import com.mixmate.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 그룹 참가자 조회와, 조 편성 전 참가자 구성(탈퇴/삭제/대리 등록)을 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final GroupRepository groupRepository;
    private final GroupBanRepository groupBanRepository;
    private final GroupMembership groupMembership;
    private final AssignmentReset assignmentReset;

    /**
     * 조 편성이 끝난 그룹의 참가자를 차수별로 조회합니다. 카드 표시에 필요한 최소 정보만 내려줍니다.
     * SECOND_ROUND는 2차까지 참석하는 인원만 포함됩니다.
     */
    @Transactional(readOnly = true)
    public ParticipantListResponse getParticipants(Long groupId, Round round, Long userId) {
        Participant me = groupMembership.getMember(groupId, userId);
        Group group = me.getGroup();

        if (round == Round.SECOND_ROUND && !group.getStatus().isSecondRoundAssigned())
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS, "2차 진행 상태에서만 조회할 수 있습니다.");
        if (me.getRole() != Role.HOST && group.getStatus().isBeforeFirstAssignment())
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS, "조 편성이 완료된 이후에 조회할 수 있습니다.");
        // 2차에 가지 않는 참가자는 2차 명단을 볼 수 없다.
        if (round == Round.SECOND_ROUND && me.getRole() != Role.HOST
                && me.getRoundParticipation() != RoundParticipation.FIRST_AND_SECOND)
            throw new CustomException(ErrorCode.FORBIDDEN, "2차 참여를 선택한 참가자만 조회할 수 있습니다.");

        List<Participant> participants = (round == Round.FIRST_ROUND)
                ? participantRepository.findByGroup(group)
                : participantRepository.findByGroupAndRoundParticipation(group, RoundParticipation.FIRST_AND_SECOND);

        return new ParticipantListResponse(participants.stream().map(ParticipantSummary::from).toList());
    }

    /**
     * 같은 그룹 참가자의 상세 프로필을 조회합니다.
     * 대상이 비공개(PRIVATE)면 거부되지만, 본인 프로필은 공개 여부와 무관하게 항상 조회됩니다.
     */
    @Transactional(readOnly = true)
    public ParticipantProfileResponse getParticipantProfile(Long groupId, Long participantId, Long userId) {
        Participant me = groupMembership.getMember(groupId, userId);

        Participant participant = participantRepository.findByParticipantIdAndGroup(participantId, me.getGroup())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "참가자를 찾을 수 없습니다."));

        boolean isSelf = participant.getParticipantId().equals(me.getParticipantId());
        if (!isSelf && participant.getProfile().getVisibility() == Visibility.PRIVATE) {
            throw new CustomException(ErrorCode.FORBIDDEN, "비공개 프로필입니다.");
        }

        return ParticipantProfileResponse.from(participant);
    }

    /**
     * 요청자 본인의 그룹 프로필을 조회합니다. 수정 화면에 기존 값을 채우는 용도라
     * 수정 요청과 같은 필드를 내려주며, 다른 참가자 조회에는 없는 공개 여부도 포함됩니다.
     */
    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(Long groupId, Long userId) {
        Participant me = groupMembership.getMember(groupId, userId);
        return MyProfileResponse.from(me);
    }

    /**
     * 요청자 본인의 그룹 프로필을 수정합니다. 프로필은 조 편성 조건으로 쓰이므로
     * 참가자 모집 중에만 수정할 수 있고, 전체 교체이므로 보내지 않은 선택 항목은 지워집니다.
     */
    @Transactional
    public void updateParticipantProfile(ParticipantProfileRequest dto, Long groupId, Long userId) {
        Participant me = groupMembership.getMember(groupId, userId);

        if (me.getGroup().getStatus() != GroupStatus.RECRUITING)
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS, "참가자 모집 중에만 프로필 수정이 가능합니다.");

        me.updateProfile(dto.toEntity());
    }

    /**
     * 일반 참가자가 그룹이 참가자 모집 중일 때 스스로 그룹을 탈퇴합니다. 관리자(HOST)는 탈퇴할 수 없고,
     * 그룹 자체를 삭제해야 합니다.
     */
    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        Participant participant = groupMembership.getMember(groupId, userId);

        if (participant.getGroup().getStatus() != GroupStatus.RECRUITING)
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS, "참가자 모집 중에만 탈퇴할 수 있습니다.");
        if (participant.getRole() == Role.HOST)
            throw new CustomException(ErrorCode.FORBIDDEN, "관리자는 탈퇴할 수 없습니다. 그룹을 삭제해 주세요.");

        participantRepository.delete(participant);
    }

    /**
     * 관리자(HOST)가 1차 진행 이전에 다른 참가자를 그룹에서 삭제합니다.
     */
    @Transactional
    public void deleteParticipant(Long groupId, Long targetParticipantId, Long userId) {
        groupRepository.findWithLockByGroupId(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "그룹정보가 없습니다."));

        Group group = groupMembership.getHost(groupId, userId).getGroup();

        if (!group.getStatus().isBeforeFirstAssignment())
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS, "1차 진행 이전에만 참가자를 삭제할 수 있습니다.");

        Participant targetParticipant = participantRepository.findByParticipantIdAndGroup(targetParticipantId, group)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "삭제대상을 찾을 수 없습니다."));
        if (targetParticipant.getRole() == Role.HOST)
            throw new CustomException(ErrorCode.FORBIDDEN, "호스트 본인은 삭제할 수 없습니다.");

        // 차단은 계정에 있는 경우에만 수행한다.
        User targetUser = targetParticipant.getUser();
        if (targetUser != null) {
            GroupBan ban = GroupBan.create(targetUser, group, targetParticipant.getProfile().getDisplayName());
            groupBanRepository.save(ban);
        }

        // 명단이 바뀐 편성은 틀린 결과다. 지운 뒤 관리자가 다시 편성한다.
        assignmentReset.resetByGroup(group);
        participantRepository.delete(targetParticipant);
    }

    @Transactional
    public void unbanUser(Long groupId, Long targetUserId, Long userId) {
        Group group = groupMembership.getHost(groupId, userId).getGroup();
        groupBanRepository.deleteByGroupAndUser_UserId(group, targetUserId);
    }

    /**
     * 관리자(HOST)가 조 편성 이전에 로그인 계정이 없는 오프라인 참가자를 대신 등록합니다.
     */
    @Transactional
    public Long addParticipant(ParticipantProfileRequest dto, Long groupId, Long userId) {
        Group group = groupMembership.getHost(groupId, userId).getGroup();

        if (!group.getStatus().isBeforeFirstAssignment())
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS, "1차 진행 이전에만 참가자를 추가할 수 있습니다.");

        Participant addedParticipant = Participant.addByHost(group, dto.toEntity());
        participantRepository.save(addedParticipant);

        // 이 참가자는 기존 편성 어디에도 없다. 그대로 두면 조가 없는 채로 확정된다.
        assignmentReset.resetByGroup(group);
        return addedParticipant.getParticipantId();
    }
}
