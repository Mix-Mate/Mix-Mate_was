package com.mixmate.domain.participant.enums;

public enum Mbti {
    ISTP, ISTJ, ISFP, ISFJ, INTP, INTJ, INFP, INFJ,
    ESTP, ESTJ, ESFP, ESFJ, ENTP, ENTJ, ENFP, ENFJ;

    public boolean isExtrovert() {
        return this.name().charAt(0) == 'E';
    }
}
// I E
// S N
// T F
// P J