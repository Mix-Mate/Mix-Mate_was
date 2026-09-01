package com.mixmate.domain.vote.entity;

import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.vote.enums.VoteChoice;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 참가자당 2차 투표(참여 의사)는 한 번만 제출할 수 있다(voter 기준 유니크).
 */
@Entity
@Table(name = "round2_participation_vote", uniqueConstraints = @UniqueConstraint(columnNames = "voter_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Round2ParticipationVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long voteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_id", nullable = false)
    private Participant voter;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private VoteChoice choice;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Round2ParticipationVote create(Participant voter, VoteChoice choice) {
        return new Round2ParticipationVote(voter, choice);
    }

    private Round2ParticipationVote(Participant voter, VoteChoice choice) {
        this.voter = voter;
        this.choice = choice;
    }

    public void updateChoice(VoteChoice choice) {
        this.choice = choice;
    }
}
