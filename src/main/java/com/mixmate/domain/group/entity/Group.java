package com.mixmate.domain.group.entity;

import com.mixmate.domain.group.enums.GroupStatus;
import com.mixmate.domain.participant.enums.Round;
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

    // 참여코드 유효 기간. 초대 링크도 이 코드를 싣기 때문에 둘의 수명은 항상 같다.
    public static final long INVITE_VALID_DAYS = 7;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupId;

    @Column(name = "group_name", nullable = false, length = 30)
    private String groupName;

    @Column(length = 120)
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GroupStatus status;

    @Column(nullable = false, unique = true, length = 8)
    private String inviteCode;

    // 참여코드를 발급한 시각. 재발급하면 갱신되므로 만료 기준은 createdAt이 아니라 이 값이다.
    @Column(nullable = false)
    private LocalDateTime inviteIssuedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Group create(String groupName, String description, String inviteCode) {
        return new Group(groupName, description, GroupStatus.RECRUITING, inviteCode);
    }

    // 참여코드와 초대 링크가 함께 죽는 시각. 호스트 화면이 남은 기간을 보여주는 데 쓴다.
    public LocalDateTime getInviteExpiresAt() {
        return inviteIssuedAt.plusDays(INVITE_VALID_DAYS);
    }

    // 참여코드를 새로 발급한다. 링크가 이 코드를 싣고 있으므로 링크도 함께 갈린다.
    public void reissueInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
        this.inviteIssuedAt = LocalDateTime.now();
    }

    public void updateInfo(String groupName, String description) {
        this.groupName = groupName;
        this.description = description;
    }

    // 참가자 모집 마감
    public void closeRecruiting() {
        this.status = GroupStatus.BEFORE_FIRST_ROUND;
    }

    // 조 편성 후 N차 시작 (진행 중으로 전이)
    public void startRound(Round round) {
        this.status = switch (round) {
            case FIRST_ROUND -> GroupStatus.FIRST_ROUND;
            case SECOND_ROUND -> GroupStatus.SECOND_ROUND;
        };
    }

    // 1차 종료하면 투표 상태로
    public void startVoting() {
        this.status = GroupStatus.VOTING;
    }

    // 관리자가 투표를 종료하면 투표마감 상태로
    public void finishVoting() {
        this.status = GroupStatus.VOTE_CLOSED;
    }

    // 2차를 진행하기로 결정하면 2차 조 편성 대기 상태로
    public void decideSecondRound() {
        this.status = GroupStatus.BEFORE_SECOND_ROUND;
    }

    // 모임을 종료하면 최종 상태로. 되돌릴 수 없다
    public void finish() {
        this.status = GroupStatus.FINISHED;
    }

    private Group(String groupName, String description, GroupStatus status, String inviteCode) {
        this.groupName = groupName;
        this.description = description;
        this.status = status;
        this.inviteCode = inviteCode;
        this.inviteIssuedAt = LocalDateTime.now();
    }
}
