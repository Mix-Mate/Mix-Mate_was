package com.mixmate.domain.assignment.entity;

import com.mixmate.domain.participant.entity.Participant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 하나의 배치(assignment) 안에서 참가자는 한 조에만 속한다(assignment_id, participant_id 유니크).
 */
@Entity
@Table(name = "team_assignment_member", uniqueConstraints = @UniqueConstraint(columnNames = {"assignment_id", "participant_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamAssignmentMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private GroupAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Column(nullable = false)
    private int teamNumber;

    @Column(nullable = false)
    private boolean isFixed;

    public static TeamAssignmentMember create(GroupAssignment assignment, Participant participant, int teamNumber, boolean isFixed) {
        return new TeamAssignmentMember(assignment, participant, teamNumber, isFixed);
    }

    private TeamAssignmentMember(GroupAssignment assignment, Participant participant, int teamNumber, boolean isFixed) {
        this.assignment = assignment;
        this.participant = participant;
        this.teamNumber = teamNumber;
        this.isFixed = isFixed;
    }
}
