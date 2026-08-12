package com.mixmate.domain.participant.service;

import com.mixmate.domain.group.entity.Group;
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
     * Group이 필요한 호출부는 반환값의 getGroup()으로 꺼내 쓰면 된다.
     * 이 메서드가 이미 같은 Group을 영속성 컨텍스트에 올려두므로 LAZY 초기화가 일어나지 않는다.
     * (같은 트랜잭션 내부에서만 성립)
     */
    public Participant getMember(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "그룹정보가 없습니다."));
        return participantRepository.findByGroupAndUser_UserId(group, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN, "그룹에 대한 참가정보가 없습니다."));
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
