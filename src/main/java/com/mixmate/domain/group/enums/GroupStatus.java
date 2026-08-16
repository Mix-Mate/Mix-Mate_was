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

    // 이 라운드의 조 편성이 확정됐는지. 참가자에게 배정 결과를 열어줄지 판단하는 기준이다.
    // 2차를 건너뛰고 종료한 그룹도 FINISHED가 되지만, 그 경우 2차 배치 자체가 없어 호출부에서 걸러진다.
    public boolean isAssignmentConfirmed(Round round) {
        return switch (round) {
            case FIRST_ROUND -> !isBeforeFirstAssignment();
            case SECOND_ROUND -> this == SECOND_ROUND || this == FINISHED;
        };
    }

    // 조 편성 확정으로 이 라운드가 이미 시작됐는지. 확정 API의 멱등 처리에 쓴다.
    public boolean isRoundInProgress(Round round) {
        return switch (round) {
            case FIRST_ROUND -> this == FIRST_ROUND;
            case SECOND_ROUND -> this == SECOND_ROUND;
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
