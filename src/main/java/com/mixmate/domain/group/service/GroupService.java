package com.mixmate.domain.group.service;

import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.auth.repository.UserRepository;
import com.mixmate.domain.group.dto.GroupCreateRequest;
import com.mixmate.domain.group.dto.GroupCreateResponse;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.group.repository.GroupRepository;
import com.mixmate.domain.participant.entity.Participant;
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
}
