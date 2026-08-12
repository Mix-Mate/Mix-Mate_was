package com.mixmate.domain.participant.service;

import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.auth.repository.UserRepository;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.group.enums.GroupStatus;
import com.mixmate.domain.group.repository.GroupRepository;
import com.mixmate.domain.participant.dto.response.ParticipantListResponse;
import com.mixmate.domain.participant.dto.ParticipantProfileDetail;
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

@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final GroupRepository groupRepository;

    @Transactional(readOnly = true)
    public ParticipantListResponse getParticipants(Long groupId, Round round, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!participantRepository.existsByGroupAndUser(group, user)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (group.getStatus() == GroupStatus.BEFORE_FIRST_ASSIGNMENT)
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS);

        List<Participant> participants = (round == Round.FIRST_ROUND)
                ? participantRepository.findByGroup(group)
                : participantRepository.findByGroupAndRoundParticipation(group, RoundParticipation.FIRST_AND_SECOND);

        return new ParticipantListResponse(participants.stream().map(ParticipantSummary::from).toList());
    }

    @Transactional(readOnly = true)
    public ParticipantProfileResponse getParticipantProfile(Long groupId, Long participantId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!participantRepository.existsByGroupAndUser(group, user)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        Participant participant = participantRepository.findByParticipantIdAndGroup(participantId, group)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        boolean isSelf = participant.getUser() != null && participant.getUser().getUserId().equals(userId);
        if (!isSelf && participant.getProfile().getVisibility() == Visibility.PRIVATE) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        return new ParticipantProfileResponse(ParticipantProfileDetail.from(participant));
    }

    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "그룹정보가 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Participant participant = participantRepository.findByGroupAndUser(group, user)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN, "그룹에 대한 참가정보가 없습니다."));

        if (group.getStatus() != GroupStatus.BEFORE_FIRST_ASSIGNMENT)
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS, "조 편성 이전에만 탈퇴할 수 있습니다.");
        if (participant.getRole() == Role.HOST)
            throw new CustomException(ErrorCode.FORBIDDEN, "관리자는 탈퇴할 수 없습니다. 그룹을 삭제해 주세요.");

        participantRepository.delete(participant);
    }
}
