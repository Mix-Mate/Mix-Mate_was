package com.mixmate.domain.vote.entity;

import com.mixmate.domain.participant.entity.Participant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 참가자당 MVP 투표는 한 표만 가능하다(voter 기준 유니크).
 * 같은 대상(target)에게는 여러 참가자가 중복으로 투표할 수 있다.
 */
@Entity
@Table(name = "mvp_vote", uniqueConstraints = @UniqueConstraint(columnNames = "voter_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MvpVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long voteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_id", nullable = false)
    private Participant voter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant target;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static MvpVote create(Participant voter, Participant target) {
        return new MvpVote(voter, target);
    }

    private MvpVote(Participant voter, Participant target) {
        this.voter = voter;
        this.target = target;
    }
}
