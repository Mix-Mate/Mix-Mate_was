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

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final GroupMembership groupMembership;

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
        Group group = groupMembership.getHost(groupId, userId).getGroup();
        if (group.getStatus() != GroupStatus.BEFORE_FIRST_ASSIGNMENT) {
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS);
        }
        group.updateInfo(dto.groupName(), dto.description());
    }

    @Transactional
    public void deleteGroup(Long groupId, Long userId) {
        Group group = groupMembership.getHost(groupId, userId).getGroup();
        if (group.getStatus() != GroupStatus.BEFORE_FIRST_ASSIGNMENT) {
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS);
        }
        participantRepository.deleteAllByGroup(group);
        groupRepository.delete(group);
    }
}
