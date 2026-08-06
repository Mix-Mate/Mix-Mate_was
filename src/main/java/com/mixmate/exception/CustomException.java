package com.mixmate.exception;

import lombok.Getter;

/**
 * ErrorCode를 담아 던지는 서비스 공통 예외입니다.
 *
 * 같은 code라도 API마다 노출 문구가 다른 경우가 있어(예: INVALID_GROUP_STATUS),
 * 문구만 바꿔야 할 때는 message를 받는 생성자를 사용합니다.
 */
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CustomException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
