package com.mixmate.domain.assignment.service;

import com.mixmate.domain.assignment.entity.GroupAssignment;
import com.mixmate.domain.assignment.repository.GroupAssignmentRepository;
import com.mixmate.domain.assignment.repository.TeamAssignmentMemberRepository;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.participant.service.AssignmentReset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 참가자 명단이 바뀌면 편성 결과를 지웁니다. 관리자는 다시 편성해야 합니다.
 *
 * 편성을 남겨두면 지워진 참가자를 가리키는 조원 행이 그대로 남고, 새로 들어온 참가자는 어느 조에도 없는 채로
 * 확정됩니다. 명단이 달라진 편성은 이미 틀린 결과이므로 고쳐 쓰지 않고 버립니다.
 */
@Service
@RequiredArgsConstructor
public class AssignmentResetter implements AssignmentReset {

    private final GroupAssignmentRepository groupAssignmentRepository;
    private final TeamAssignmentMemberRepository teamAssignmentMemberRepository;

    @Override
    @Transactional
    public void resetByGroup(Group group) {
        List<GroupAssignment> assignments = groupAssignmentRepository.findByGroup(group);

        // 조원을 먼저 지워야 참가자를 지울 때 외래 키에 걸리지 않는다. 벌크 삭제라 즉시 DB에 반영된다.
        assignments.forEach(teamAssignmentMemberRepository::deleteAllByAssignment);
        groupAssignmentRepository.deleteAll(assignments);
    }
}
