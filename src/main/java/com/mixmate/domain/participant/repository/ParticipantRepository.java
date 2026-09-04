package com.mixmate.domain.participant.repository;

import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.group.enums.GroupStatus;
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

    /**
     * 참가 정보와 그 그룹을 한 번에 가져온다. Group을 먼저 조회해 넘길 필요가 없어
     * 그룹 스코프 API가 공통으로 내던 SELECT 두 번이 한 번으로 줄어든다.
     * 결과가 비면 그룹이 없는 것인지 참가자가 아닌 것인지 구분되지 않으므로,
     * 404와 403을 갈라야 하는 호출부는 그때만 그룹 존재 여부를 따로 확인해야 한다.
     */
    @Query("select p from Participant p join fetch p.group "
            + "where p.group.groupId = :groupId and p.user.userId = :userId")
    Optional<Participant> findWithGroupByGroupIdAndUserId(@Param("groupId") Long groupId,
                                                          @Param("userId") Long userId);

    List<Participant> findByGroupAndRoundParticipation(Group group, RoundParticipation roundParticipation);

    long countByGroupAndRoundParticipation(Group group, RoundParticipation roundParticipation);

    Optional<Participant> findByParticipantIdAndGroup(Long participantId, Group group);

    /** scope=me&state=active — FINISHED를 제외한 그룹만 조회할 때 사용 */
    List<Participant> findByUserAndGroup_StatusNot(User user, GroupStatus status);

    /** scope=me&state=finished — FINISHED인 그룹만 조회할 때 사용 */
    List<Participant> findByUserAndGroup_Status(User user, GroupStatus status);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Participant p WHERE p.group = :group")
    void deleteAllByGroup(@Param("group") Group group);
}
