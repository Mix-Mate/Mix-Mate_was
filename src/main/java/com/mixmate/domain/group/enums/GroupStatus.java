package com.mixmate.domain.group.enums;

import com.mixmate.domain.participant.enums.Round;

public enum GroupStatus {
    RECRUITING, BEFORE_FIRST_ROUND, FIRST_ROUND, VOTING, VOTE_CLOSED,
    BEFORE_SECOND_ROUND, SECOND_ROUND, FINISHED;

    public boolean isSecondRoundAssigned() {
        return switch (this) {
            case BEFORE_SECOND_ROUND, SECOND_ROUND, FINISHED -> true;
            case RECRUITING, BEFORE_FIRST_ROUND, FIRST_ROUND, VOTING, VOTE_CLOSED -> false;
        };
    }

    public boolean isBeforeFirstAssignment() {
        return switch (this) {
            case RECRUITING, BEFORE_FIRST_ROUND -> true;
            case FIRST_ROUND, VOTING, VOTE_CLOSED, BEFORE_SECOND_ROUND, SECOND_ROUND, FINISHED -> false;
        };
    }

    public boolean canAssign(Round round) {
        return switch (round) {
            case FIRST_ROUND -> this == BEFORE_FIRST_ROUND;
            case SECOND_ROUND -> this == BEFORE_SECOND_ROUND;
        };
    }

    public boolean isVoteFinished() {
        return switch (this) {
            case VOTE_CLOSED, BEFORE_SECOND_ROUND, SECOND_ROUND, FINISHED -> true;
            case RECRUITING, BEFORE_FIRST_ROUND, FIRST_ROUND, VOTING -> false;
        };
    }

    // finished 상태에서 멱등으로 처리할 때는 서비스레이어 조기 반환으로 처리
    public boolean canFinish() {
        return switch (this) {
            case VOTE_CLOSED, SECOND_ROUND -> true;
            case RECRUITING, BEFORE_FIRST_ROUND, FIRST_ROUND, VOTING, BEFORE_SECOND_ROUND, FINISHED -> false;
        };
    }
}
