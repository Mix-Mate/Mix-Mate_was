package com.mixmate.domain.participant.repository;

import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.group.enums.GroupStatus;
import com.mixmate.domain.participant.dto.ParticipantProfileDetail;
import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.enums.RoundParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    List<Participant> findByGroup(Group group);

    long countByGroup(Group group);

    boolean existsByGroupAndUser(Group group, User user);

    Optional<Participant> findByGroupAndUser(Group group, User user);

    List<Participant> findByGroupAndRoundParticipation(Group group, RoundParticipation roundParticipation);

    Optional<Participant> findByParticipantIdAndGroup(Long participantId, Group group);

    /** scope=me&state=active — FINISHED를 제외한 그룹만 조회할 때 사용 */
    List<Participant> findByUserAndGroup_StatusNot(User user, GroupStatus status);

    /** scope=me&state=finished — FINISHED인 그룹만 조회할 때 사용 */
    List<Participant> findByUserAndGroup_Status(User user, GroupStatus status);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Participant p WHERE p.group = :group")
    void deleteAllByGroup(@Param("group") Group group);
}
