package com.mixmate.domain.participant.service;

import com.mixmate.domain.group.entity.Group;

/**
 * 참가자 명단이 바뀌었을 때 그 그룹의 조 편성 결과를 지우는 통로입니다.
 * 구현은 assignment 도메인에 있습니다. 선언을 participant에 두어 두 도메인이 서로를 참조하지 않게 합니다.
 */
public interface AssignmentReset {

    /** 확정 여부와 무관하게 해당 그룹의 모든 차수 편성을 지웁니다. 편성이 없으면 아무 일도 하지 않습니다. */
    void resetByGroup(Group group);
}
