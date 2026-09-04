package com.mixmate.domain.participant.service;

import com.mixmate.domain.group.repository.GroupRepository;
import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.enums.Role;
import com.mixmate.domain.participant.repository.ParticipantRepository;
import com.mixmate.exception.CustomException;
import com.mixmate.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 그룹 단위 API가 공통으로 거치는 두 가지 검증을 모아뒀다.
 */
@Component
@RequiredArgsConstructor
public class GroupMembership {

    private final GroupRepository groupRepository;
    private final ParticipantRepository participantRepository;

    /**
     * 요청자가 이 그룹의 참가자인지 확인하고 그 참가 정보를 돌려준다.
     *
     * 조회 쿼리가 fetch join으로 Group까지 함께 채워오므로, Group이 필요한 호출부는
     * 반환값의 getGroup()을 그냥 꺼내 쓰면 된다. LAZY 초기화가 일어나지 않는다.
     * 반면 Participant.user는 여전히 LAZY이므로 getUser()는 트랜잭션 안에서만 안전하다.
     *
     * 정상 경로는 SELECT 한 번으로 끝난다.
     */
    public Participant getMember(Long groupId, Long userId) {
        return participantRepository.findWithGroupByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> memberNotFound(groupId));
    }

    /**
     * 조회가 비었을 때만 호출된다. 그룹 자체가 없는 것(404)과, 그룹은 있지만 요청자가
     * 참가자가 아닌 것(403)을 여기서 가른다. 실패 경로에서만 그룹을 한 번 더 조회하므로
     * 정상 요청은 SELECT 한 번으로 끝난다.
     */
    private CustomException memberNotFound(Long groupId) {
        if (groupRepository.existsById(groupId)) {
            return new CustomException(ErrorCode.FORBIDDEN, "그룹에 대한 참가정보가 없습니다.");
        }
        return new CustomException(ErrorCode.NOT_FOUND, "그룹정보가 없습니다.");
    }

    /**
     * 관리자 전용 API의 공통 진입점. 요청자가 HOST가 아니면 막는다.
     */
    public Participant getHost(Long groupId, Long userId) {
        Participant participant = getMember(groupId, userId);
        if (participant.getRole() != Role.HOST) {
            throw new CustomException(ErrorCode.NOT_GROUP_ADMIN);
        }
        return participant;
    }
}
