package com.mixmate.domain.participant.service;

import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.group.enums.GroupStatus;
import com.mixmate.domain.participant.dto.request.ParticipantProfileRequest;
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

    private final ParticipantRepository participantRepository;
    private final GroupMembership groupMembership;

    @Transactional(readOnly = true)
    public ParticipantListResponse getParticipants(Long groupId, Round round, Long userId) {
        Group group = groupMembership.getMember(groupId, userId).getGroup();

        if (group.getStatus() == GroupStatus.BEFORE_FIRST_ASSIGNMENT)
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS);

        List<Participant> participants = (round == Round.FIRST_ROUND)
                ? participantRepository.findByGroup(group)
                : participantRepository.findByGroupAndRoundParticipation(group, RoundParticipation.FIRST_AND_SECOND);

        return new ParticipantListResponse(participants.stream().map(ParticipantSummary::from).toList());
    }

    @Transactional(readOnly = true)
    public ParticipantProfileResponse getParticipantProfile(Long groupId, Long participantId, Long userId) {
        Participant me = groupMembership.getMember(groupId, userId);

        Participant participant = participantRepository.findByParticipantIdAndGroup(participantId, me.getGroup())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        boolean isSelf = participant.getParticipantId().equals(me.getParticipantId());
        if (!isSelf && participant.getProfile().getVisibility() == Visibility.PRIVATE) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        return new ParticipantProfileResponse(ParticipantProfileDetail.from(participant));
    }

    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        Participant participant = groupMembership.getMember(groupId, userId);

        if (participant.getGroup().getStatus() != GroupStatus.BEFORE_FIRST_ASSIGNMENT)
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS, "조 편성 이전에만 탈퇴할 수 있습니다.");
        if (participant.getRole() == Role.HOST)
            throw new CustomException(ErrorCode.FORBIDDEN, "관리자는 탈퇴할 수 없습니다. 그룹을 삭제해 주세요.");

        participantRepository.delete(participant);
    }

    @Transactional
    public void deleteParticipant(Long groupId, Long targetParticipantId, Long userId) {
        Group group = groupMembership.getHost(groupId, userId).getGroup();

        if (group.getStatus() != GroupStatus.BEFORE_FIRST_ASSIGNMENT)
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS, "조 편성 이전에만 참가자를 삭제할 수 있습니다.");

        Participant targetParticipant = participantRepository.findByParticipantIdAndGroup(targetParticipantId, group)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "삭제대상을 찾을 수 없습니다."));
        if (targetParticipant.getRole() == Role.HOST)
            throw new CustomException(ErrorCode.FORBIDDEN, "호스트 본인은 삭제할 수 없습니다.");

        participantRepository.delete(targetParticipant);
    }

    @Transactional
    public Long addParticipant(ParticipantProfileRequest dto, Long groupId, Long userId) {
        Group group = groupMembership.getHost(groupId, userId).getGroup();

        if (group.getStatus() != GroupStatus.BEFORE_FIRST_ASSIGNMENT)
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS, "조 편성 이전에만 참가자를 추가할 수 있습니다.");

        Participant addedParticipant = Participant.addByHost(group, dto.toEntity());
        participantRepository.save(addedParticipant);
        return addedParticipant.getParticipantId();
    }
}
