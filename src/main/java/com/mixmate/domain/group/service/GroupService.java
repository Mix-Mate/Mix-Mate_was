package com.mixmate.domain.group.service;

import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.auth.repository.UserRepository;
import com.mixmate.domain.group.dto.request.GroupCreateRequest;
import com.mixmate.domain.group.dto.response.GroupCreateResponse;
import com.mixmate.domain.group.dto.request.GroupUpdateRequest;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.group.enums.GroupStatus;
import com.mixmate.domain.group.repository.GroupRepository;
import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.repository.ParticipantRepository;
import com.mixmate.domain.participant.service.GroupMembership;
import com.mixmate.exception.CustomException;
import com.mixmate.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 그룹 생성, 정보 수정, 삭제를 관리자(HOST) 권한 기준으로 처리하는 서비스입니다.
 * 그룹 참여코드 검증/입장은 이 클래스가 아니라 home 패키지의 HomeGroupService가 담당합니다.
 */
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final GroupMembership groupMembership;

    private final static int MAX_LOOP_COUNT = 5;

    /**
     * 새 그룹을 생성하고, 생성자를 관리자(HOST) 겸 첫 참가자로 등록합니다.
     * 참여코드는 중복되지 않을 때까지 최대 MAX_LOOP_COUNT번 재생성을 시도합니다.
     *
     * @param dto 그룹명, 설명, 생성자 본인의 프로필
     * @param userId 그룹을 생성하는 사용자 식별자
     * @return 생성된 그룹의 식별자, 이름, 참여코드
     */
    @Transactional
    public GroupCreateResponse createGroup(GroupCreateRequest dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String inviteCode = null;
        for (int i = 0; i < MAX_LOOP_COUNT; i++) {
            String candidate = inviteCodeGenerator.generate();
            if (!groupRepository.existsByInviteCode(candidate)) {
                inviteCode = candidate;
                break;
            }
        }
        if (inviteCode == null) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        Group group = groupRepository.save(Group.create(dto.groupName(), dto.description(), inviteCode));
        Participant host = Participant.createHost(user, group, dto.profile().toEntity());
        participantRepository.save(host);
        return new GroupCreateResponse(group.getGroupId(), group.getGroupName(), group.getInviteCode());
    }

    /**
     * 관리자가 조 편성 전에 그룹명과 설명을 수정합니다.
     *
     * @param dto 새 그룹명과 설명
     * @param groupId 수정할 그룹 식별자
     * @param userId 요청자 식별자 (해당 그룹의 HOST여야 함)
     */
    @Transactional
    public void updateGroup(GroupUpdateRequest dto, Long groupId, Long userId) {
        Group group = groupMembership.getHost(groupId, userId).getGroup();
        if (group.getStatus() != GroupStatus.BEFORE_FIRST_ASSIGNMENT) {
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS, "조 편성 이전에만 수정할 수 있습니다.");
        }
        group.updateInfo(dto.groupName(), dto.description());
    }

    /**
     * 관리자가 조 편성 전에 그룹과 그 참가자 전체를 삭제합니다. 되돌릴 수 없습니다.
     *
     * @param groupId 삭제할 그룹 식별자
     * @param userId 요청자 식별자 (해당 그룹의 HOST여야 함)
     */
    @Transactional
    public void deleteGroup(Long groupId, Long userId) {
        Group group = groupMembership.getHost(groupId, userId).getGroup();
        if (group.getStatus() != GroupStatus.BEFORE_FIRST_ASSIGNMENT) {
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS, "조 편성 이전에만 삭제할 수 있습니다.");
        }
        participantRepository.deleteAllByGroup(group);
        groupRepository.delete(group);
    }
}
