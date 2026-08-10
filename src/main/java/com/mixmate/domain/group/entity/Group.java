package com.mixmate.domain.group.entity;

import com.mixmate.domain.group.enums.GroupStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "event_group")
@EntityListeners(AuditingEntityListener.class)
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupId;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Column
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GroupStatus status;

    @Column(nullable = false, unique = true, length = 8)
    private String inviteCode;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Group create(String groupName, String description, String inviteCode) {
        return new Group(groupName, description, GroupStatus.BEFORE_FIRST_ASSIGNMENT, inviteCode);
    }

    public void updateInfo(String groupName, String description) {
        this.groupName = groupName;
        this.description = description;
    }

    private Group(String groupName, String description, GroupStatus status, String inviteCode) {
        this.groupName = groupName;
        this.description = description;
        this.status = status;
        this.inviteCode = inviteCode;
    }
}
