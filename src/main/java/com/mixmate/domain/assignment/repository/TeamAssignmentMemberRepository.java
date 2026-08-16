package com.mixmate.domain.assignment.repository;

import com.mixmate.domain.assignment.entity.GroupAssignment;
import com.mixmate.domain.assignment.entity.TeamAssignmentMember;
import com.mixmate.domain.participant.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TeamAssignmentMemberRepository extends JpaRepository<TeamAssignmentMember, Long> {

    Optional<TeamAssignmentMember> findByAssignmentAndParticipant(GroupAssignment assignment, Participant participant);

    // 재편성 시 기존 조원을 지운다. 같은 참가자가 다시 삽입되므로 DELETE가 INSERT보다 먼저 DB에 닿아야 한다.
    // 파생 쿼리는 flush 시점까지 삭제를 미뤄 (assignment_id, participant_id) 유니크에 걸린다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from TeamAssignmentMember m where m.assignment = :assignment")
    void deleteAllByAssignment(@Param("assignment") GroupAssignment assignment);
}
