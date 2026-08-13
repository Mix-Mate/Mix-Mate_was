package com.mixmate.domain.assignment.entity;

import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.participant.enums.Round;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 그룹당 라운드(1차/2차)마다 한 번의 조 편성 배치만 존재한다(group_id, round_number 유니크).
 * 재편성이 필요하면 기존 배치를 finalized 이전 상태로 두고 갱신하는 것을 전제로 한다.
 */
@Entity
@Table(name = "group_assignment", uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "round_number"}))
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

    @Column(name = "round_number", nullable = false)
    @Enumerated(EnumType.STRING)
    private Round round;

    @Column(nullable = false)
    private int teamSize;

    @Column
    private String conditions;

    @Column(nullable = false)
    private boolean finalized;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static GroupAssignment create(Group group, Round round, int teamSize, String conditions) {
        return new GroupAssignment(group, round, teamSize, conditions);
    }

    private GroupAssignment(Group group, Round round, int teamSize, String conditions) {
        this.group = group;
        this.round = round;
        this.teamSize = teamSize;
        this.conditions = conditions;
        this.finalized = false;
    }

    public void finalizeAssignment() {
        this.finalized = true;
    }
}
