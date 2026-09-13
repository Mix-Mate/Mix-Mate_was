package com.mixmate.domain.participant.entity;

import com.mixmate.domain.participant.enums.*;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ParticipantProfile {

    @Column(nullable = false, length = 10)
    private String displayName;

    @Column(nullable = false, length = 20)
    private String studentId;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private Position position;

    @Column(nullable = false, length = 20)
    private String major;

    @Column(nullable = false)
    private boolean isNew;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private Grade grade;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private Gender gender;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private Mbti mbti;

    @Column
    private Integer age;

    @Column(length = 30)
    private String instaId;

    @Column(length = 120)
    private String bio;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private Visibility visibility;
}
