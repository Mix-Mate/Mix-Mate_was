package com.mixmate.domain.participant.service;

import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.auth.repository.UserRepository;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.group.enums.GroupStatus;
import com.mixmate.domain.group.repository.GroupRepository;
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

/**
 * 그룹 참가자 조회와, 조 편성 전 참가자 구성(탈퇴/삭제/대리 등록)을 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final GroupRepository groupRepository;

    /**
     * 조 편성이 끝난 그룹의 참가자를 차수별로 조회합니다. 카드 표시에 필요한 최소 정보만 내려줍니다.
     * SECOND_ROUND는 2차까지 참석하는 인원만 포함됩니다.
     *
     * @param groupId 조회할 그룹 식별자
     * @param round 조회할 차수
     * @param userId 요청자 식별자 (이 그룹의 참가자여야 함)
     * @return 참가자 요약 목록
     */
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

    /**
     * 같은 그룹 참가자의 상세 프로필을 조회합니다.
     * 대상이 비공개(PRIVATE)면 거부되지만, 본인 프로필은 공개 여부와 무관하게 항상 조회됩니다.
     *
     * @param groupId 조회할 그룹 식별자
     * @param participantId 조회 대상 참가자 식별자
     * @param userId 요청자 식별자 (이 그룹의 참가자여야 함)
     * @return 대상의 상세 프로필
     */
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

    /**
     * 일반 참가자가 조 편성 이전에 스스로 그룹을 탈퇴합니다. 관리자(HOST)는 탈퇴할 수 없고,
     * 그룹 자체를 삭제해야 합니다.
     *
     * @param groupId 탈퇴할 그룹 식별자
     * @param userId 탈퇴하려는 사용자 식별자
     */
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

    /**
     * 관리자(HOST)가 조 편성 이전에 다른 참가자를 그룹에서 삭제합니다. 호스트 본인은 이 방법으로
     * 삭제할 수 없습니다.
     *
     * @param groupId 요청 그룹 식별자
     * @param targetParticipantId 삭제 대상 참가자 식별자
     * @param userId 요청자 식별자 (이 그룹의 HOST여야 함)
     */
    @Transactional
    public void deleteParticipant(Long groupId, Long targetParticipantId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "그룹정보가 없습니다."));
        Participant participant = participantRepository.findByGroupAndUser(group, user)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN, "그룹에 대한 참가정보가 없습니다."));

        if (group.getStatus() != GroupStatus.BEFORE_FIRST_ASSIGNMENT)
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS, "조 편성 이전에만 참가자를 삭제할 수 있습니다.");
        if (participant.getRole() != Role.HOST)
            throw new CustomException(ErrorCode.NOT_GROUP_ADMIN);

        Participant targetParticipant = participantRepository.findByParticipantIdAndGroup(targetParticipantId, group)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "삭제대상을 찾을 수 없습니다."));
        if (targetParticipant.getRole() == Role.HOST)
            throw new CustomException(ErrorCode.FORBIDDEN, "호스트 본인은 삭제할 수 없습니다.");

        participantRepository.delete(targetParticipant);
    }

    /**
     * 관리자(HOST)가 조 편성 이전에 로그인 계정이 없는 오프라인 참가자를 대신 등록합니다.
     *
     * @param dto 추가할 참가자의 프로필
     * @param groupId 요청 그룹 식별자
     * @param userId 요청자 식별자 (이 그룹의 HOST여야 함)
     * @return 새로 등록된 참가자의 식별자
     */
    @Transactional
    public Long addParticipant(ParticipantProfileRequest dto, Long groupId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "그룹정보가 없습니다."));
        Participant participant = participantRepository.findByGroupAndUser(group, user)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN, "그룹에 대한 참가정보가 없습니다."));

        if (group.getStatus() != GroupStatus.BEFORE_FIRST_ASSIGNMENT)
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS, "조 편성 이전에만 참가자를 추가할 수 있습니다.");
        if (participant.getRole() != Role.HOST)
            throw new CustomException(ErrorCode.NOT_GROUP_ADMIN);

        Participant addedParticipant = Participant.addByHost(group, dto.toEntity());
        participantRepository.save(addedParticipant);
        return addedParticipant.getParticipantId();
    }
}
