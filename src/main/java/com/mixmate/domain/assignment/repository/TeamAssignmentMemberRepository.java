package com.mixmate.domain.assignment.repository;

import com.mixmate.domain.assignment.entity.GroupAssignment;
import com.mixmate.domain.assignment.entity.TeamAssignmentMember;
import com.mixmate.domain.participant.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamAssignmentMemberRepository extends JpaRepository<TeamAssignmentMember, Long> {

    Optional<TeamAssignmentMember> findByAssignmentAndParticipant(GroupAssignment assignment, Participant participant);

    // 조원 프로필까지 응답에 나가므로 participant를 함께 로딩한다. fetch 없이는 조원 수만큼 추가 쿼리가 나간다.
    @Query("select m from TeamAssignmentMember m join fetch m.participant "
            + "where m.assignment = :assignment and m.teamNumber = :teamNumber")
    List<TeamAssignmentMember> findByAssignmentAndTeamNumber(@Param("assignment") GroupAssignment assignment,
                                                             @Param("teamNumber") int teamNumber);

    // 재편성 시 기존 조원을 지운다. 같은 참가자가 다시 삽입되므로 DELETE가 INSERT보다 먼저 DB에 닿아야 한다.
    // 파생 쿼리는 flush 시점까지 삭제를 미뤄 (assignment_id, participant_id) 유니크에 걸린다.
    @Modifying(flushAutomatically = true)
    @Query("delete from TeamAssignmentMember m where m.assignment = :assignment")
    void deleteAllByAssignment(@Param("assignment") GroupAssignment assignment);
}
