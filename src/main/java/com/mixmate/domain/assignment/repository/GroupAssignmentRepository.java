package com.mixmate.domain.assignment.repository;

import com.mixmate.domain.assignment.entity.GroupAssignment;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.participant.enums.Round;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupAssignmentRepository extends JpaRepository<GroupAssignment, Long> {

    Optional<GroupAssignment> findByGroupAndRound(Group group, Round round);

    /** 1·2차 배치가 각각 한 행씩 있을 수 있어 List로 받는다. */
    List<GroupAssignment> findByGroup(Group group);
}
