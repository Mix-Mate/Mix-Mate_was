package com.mixmate.domain.participant.entity;

import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.participant.enums.Role;
import com.mixmate.domain.participant.enums.RoundParticipation;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * (group_id, user_id) 유니크 제약은 같은 유저의 중복 입장을 막는 마지막 방어선이다.
 * 서비스에서 먼저 검사하지만 동시 요청 두 건은 둘 다 통과할 수 있어 DB에서도 막는다.
 * user_id가 NULL인 행(관리자 대리 등록 참가자)은 MySQL이 중복으로 치지 않으므로 한 그룹에 여러 명이 들어갈 수 있다.
 */
@Entity
@Table(name = "participant", uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long participantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Embedded
    private ParticipantProfile profile;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private RoundParticipation roundParticipation;

    /**
     * 프로필을 마지막으로 입력·수정한 시각.
     * 행이 아니라 프로필만의 수정일이므로 @LastModifiedDate를 쓰지 않고 직접 찍는다.
     * (roundParticipation 변경처럼 프로필과 무관한 수정에는 갱신되면 안 된다)
     */
    @Column(nullable = false)
    private LocalDateTime profileUpdatedAt;

    public static Participant createHost(User user, Group group, ParticipantProfile profile) {
        return new Participant(user, group, profile, Role.HOST);
    }
    public static Participant join(User user, Group group, ParticipantProfile profile) {
        return new Participant(user, group, profile, Role.PARTICIPANT);
    }
    /** 관리자가 대리 등록한 오프라인 참가자. 로그인 계정이 없으므로 user는 null이다. */
    public static Participant addByHost(Group group, ParticipantProfile profile) {
        return new Participant(null, group, profile, Role.PARTICIPANT);
    }

    private Participant(User user, Group group, ParticipantProfile profile, Role role) {
        this.user = user;
        this.group = group;
        this.profile = profile;
        this.role = role;
        this.roundParticipation = RoundParticipation.FIRST_ONLY;
        this.profileUpdatedAt = LocalDateTime.now();
    }

    public void updateProfile(ParticipantProfile profile) {
        this.profile = profile;
        this.profileUpdatedAt = LocalDateTime.now();
    }

    public void joinSecondRound() {
        this.roundParticipation = RoundParticipation.FIRST_AND_SECOND;
    }
}
