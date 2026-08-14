package com.mixmate.domain.group.enums;

public enum GroupStatus {
    BEFORE_FIRST_ASSIGNMENT, FIRST_ROUND, VOTING, VOTE_CLOSED,
    BEFORE_SECOND_ASSIGNMENT, SECOND_ROUND, FINISHED;

    public boolean isSecondRoundAssigned() {
        return switch (this) {
            case BEFORE_SECOND_ASSIGNMENT, SECOND_ROUND, FINISHED -> true;
            case BEFORE_FIRST_ASSIGNMENT, FIRST_ROUND, VOTING, VOTE_CLOSED -> false;
        };
    }

    // finished 상태에서 멱등으로 처리할 때는 서비스레이어 조기 반환으로 처리
    public boolean canFinish() {
        return switch (this) {
            case VOTE_CLOSED, SECOND_ROUND -> true;
            case BEFORE_FIRST_ASSIGNMENT, FIRST_ROUND, VOTING, BEFORE_SECOND_ASSIGNMENT, FINISHED -> false;
        };
    }
}
