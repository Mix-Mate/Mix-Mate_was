package com.mixmate.domain.assignment.entity;

import com.mixmate.domain.assignment.enums.AssignmentCondition;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.participant.enums.Round;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 그룹당 라운드(1차/2차)마다 한 번의 조 편성 배치만 존재한다(group_id, round 유니크).
 * 배치의 확정 여부는 이 엔티티가 아니라 그룹 상태(GroupStatus)가 나타낸다.
 */
@Entity
@Table(name = "group_assignment", uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "round"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class GroupAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(name = "round", nullable = false)
    @Enumerated(EnumType.STRING)
    private Round round;

    @Column(nullable = false)
    private int teamCount;

    @ElementCollection
    @CollectionTable(name = "group_assignment_condition",
            joinColumns = @JoinColumn(name = "assignment_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "condition_name", nullable = false)
    private Set<AssignmentCondition> conditions = new HashSet<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static GroupAssignment create(Group group, Round round, int teamCount, Set<AssignmentCondition> conditions) {
        return new GroupAssignment(group, round, teamCount, conditions);
    }

    private GroupAssignment(Group group, Round round, int teamCount, Set<AssignmentCondition> conditions) {
        this.group = group;
        this.round = round;
        this.teamCount = teamCount;
        this.conditions = new HashSet<>(conditions);
    }
}
