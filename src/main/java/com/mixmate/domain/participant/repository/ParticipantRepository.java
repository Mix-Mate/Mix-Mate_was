package com.mixmate.domain.participant.repository;

import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.group.entity.Group;
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

    boolean existsByGroupAndUser(Group group, User user);

    Optional<Participant> findByGroupAndUser(Group group, User user);

    /**
     * 요청자의 참가 정보를 userId만으로 찾는다.
     * User 엔티티를 먼저 조회할 필요가 없어 요청당 SELECT 한 번이 줄어든다.
     */
    Optional<Participant> findByGroupAndUser_UserId(Group group, Long userId);

    List<Participant> findByGroupAndRoundParticipation(Group group, RoundParticipation roundParticipation);

    Optional<Participant> findByParticipantIdAndGroup(Long participantId, Group group);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Participant p WHERE p.group = :group")
    void deleteAllByGroup(@Param("group") Group group);
}
