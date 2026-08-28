package com.mixmate.domain.group.repository;

import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.group.entity.GroupBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupBanRepository extends JpaRepository<GroupBan, Long> {

    boolean existsByGroupAndUser(Group group, User user);

    // 응답에 이메일이 들어가 user 프록시가 반드시 초기화된다. 함께 가져오지 않으면 차단 수만큼 SELECT가 더 나간다.
    @Query("SELECT b FROM GroupBan b JOIN FETCH b.user WHERE b.group = :group ORDER BY b.createdAt DESC")
    List<GroupBan> findAllByGroupWithUser(@Param("group") Group group);

    long deleteByGroupAndUser_UserId(Group group, Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM GroupBan b WHERE b.group = :group")
    void deleteAllByGroup(@Param("group") Group group);
}
