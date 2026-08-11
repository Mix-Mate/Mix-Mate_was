package com.mixmate.domain.group.service;

import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.auth.repository.UserRepository;
import com.mixmate.domain.group.dto.GroupCreateRequest;
import com.mixmate.domain.group.dto.GroupCreateResponse;
import com.mixmate.domain.group.dto.GroupUpdateRequest;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.group.enums.GroupStatus;
import com.mixmate.domain.group.repository.GroupRepository;
import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.enums.Role;
import com.mixmate.domain.participant.repository.ParticipantRepository;
import com.mixmate.exception.CustomException;
import com.mixmate.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final InviteCodeGenerator inviteCodeGenerator;

    private final static int MAX_LOOP_COUNT = 5;

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

    @Transactional
    public void updateGroup(GroupUpdateRequest dto, Long groupId, Long userId) {
        Group group = getGroupAsHost(groupId, userId);
        if (group.getStatus() != GroupStatus.BEFORE_FIRST_ASSIGNMENT) {
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS);
        }
        group.updateInfo(dto.groupName(), dto.description());
    }

    @Transactional
    public void deleteGroup(Long groupId, Long userId) {
        Group group = getGroupAsHost(groupId, userId);
        if (group.getStatus() != GroupStatus.BEFORE_FIRST_ASSIGNMENT) {
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS);
        }
        participantRepository.deleteAllByGroup(group);
        groupRepository.delete(group);
    }

    /**
     * 요청자 본인의 참가 정보를 꺼내면서, 이 그룹의 참가자가 맞는지 함께 검증한다.
     */
    private Participant getMyParticipant(Long groupId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        return participantRepository.findByGroupAndUser(group, user)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    /**
     * 관리자 전용 API의 공통 진입점. 요청자가 HOST가 아니면 막고, 검증된 그룹을 돌려준다.
     *
     * getMyParticipant가 이미 같은 Group을 영속성 컨텍스트에 올려두므로,
     * getGroup()을 불러도 LAZY 초기화가 일어나지 않는다. (같은 트랜잭션 내부에서만 성립)
     */
    private Group getGroupAsHost(Long groupId, Long userId) {
        Participant participant = getMyParticipant(groupId, userId);
        if (participant.getRole() != Role.HOST) {
            throw new CustomException(ErrorCode.NOT_GROUP_ADMIN);
        }
        return participant.getGroup();
    }
}
