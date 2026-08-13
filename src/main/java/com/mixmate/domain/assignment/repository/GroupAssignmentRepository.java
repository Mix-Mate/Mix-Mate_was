package com.mixmate.domain.assignment.repository;

import com.mixmate.domain.assignment.entity.GroupAssignment;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.participant.enums.Round;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupAssignmentRepository extends JpaRepository<GroupAssignment, Long> {

    Optional<GroupAssignment> findByGroupAndRound(Group group, Round round);
}
