package com.mixmate.domain.group.repository;

import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.group.entity.GroupBan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupBanRepository extends JpaRepository<GroupBan, Long> {

    boolean existsByGroupAndUser(Group group, User user);

    List<GroupBan> findByGroupOrderByCreatedAtDesc(Group group);

    long deleteByGroupAndUser_UserId(Group group, Long userId);
}
