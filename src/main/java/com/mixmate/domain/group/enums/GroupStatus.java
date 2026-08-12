package com.mixmate.domain.group.enums;

public enum GroupStatus {
    BEFORE_FIRST_ASSIGNMENT, FIRST_ROUND, VOTING,
    BEFORE_SECOND_ASSIGNMENT, SECOND_ROUND, FINISHED;

    public boolean isSecondRoundAssigned() {
        return switch (this) {
            case BEFORE_SECOND_ASSIGNMENT, SECOND_ROUND, FINISHED -> true;
            case BEFORE_FIRST_ASSIGNMENT, FIRST_ROUND, VOTING -> false;
        };
    }
}
