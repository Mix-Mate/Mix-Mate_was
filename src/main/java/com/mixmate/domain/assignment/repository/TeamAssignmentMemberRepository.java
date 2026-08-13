package com.mixmate.domain.assignment.repository;

import com.mixmate.domain.assignment.entity.GroupAssignment;
import com.mixmate.domain.assignment.entity.TeamAssignmentMember;
import com.mixmate.domain.participant.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamAssignmentMemberRepository extends JpaRepository<TeamAssignmentMember, Long> {

    Optional<TeamAssignmentMember> findByAssignmentAndParticipant(GroupAssignment assignment, Participant participant);
}
