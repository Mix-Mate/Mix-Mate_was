package com.mixmate.domain.home.service;

import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.auth.repository.UserRepository;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.group.enums.GroupStatus;
import com.mixmate.domain.group.repository.GroupRepository;
import com.mixmate.domain.home.dto.request.HomeGroupJoinReqDto;
import com.mixmate.domain.home.dto.request.HomeInviteCodeVerifyReqDto;
import com.mixmate.domain.home.dto.response.HomeInviteCodeVerifyResDto;
import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.repository.ParticipantRepository;
import com.mixmate.exception.CustomException;
import com.mixmate.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HomeGroupService {

    private static final long INVITE_CODE_VALID_DAYS = 3;

    private final GroupRepository groupRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public HomeInviteCodeVerifyResDto verifyInviteCode(HomeInviteCodeVerifyReqDto dto) {
        Group group = findValidGroupByInviteCode(dto.getInviteCode());
        return HomeInviteCodeVerifyResDto.fromEntity(group);
    }

    @Transactional
    public HomeInviteCodeVerifyResDto joinGroup(HomeGroupJoinReqDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Group group = findValidGroupByInviteCode(dto.getInviteCode());

        if (group.getStatus() != GroupStatus.BEFORE_FIRST_ASSIGNMENT) {
            throw new CustomException(ErrorCode.GROUP_LOCKED);
        }
        if (participantRepository.existsByGroupAndUser(group, user)) {
            throw new CustomException(ErrorCode.ALREADY_JOINED);
        }

        Participant participant = Participant.join(user, group, dto.getProfile().toEntity());
        participantRepository.save(participant);
        return HomeInviteCodeVerifyResDto.fromEntity(group);
    }

    /**
     * 참여코드는 그룹 생성일로부터 INVITE_CODE_VALID_DAYS일 동안만 유효하다.
     * 만료된 코드는 존재하지 않는 코드와 동일하게 취급해 사용자에게 같은 에러를 보여준다.
     */
    private Group findValidGroupByInviteCode(String inviteCode) {
        Group group = groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INVITE_CODE));

        LocalDateTime expiresAt = group.getCreatedAt().plusDays(INVITE_CODE_VALID_DAYS);
        if (expiresAt.isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.INVALID_INVITE_CODE);
        }
        return group;
    }
}
