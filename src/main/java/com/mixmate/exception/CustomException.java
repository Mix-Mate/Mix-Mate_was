package com.mixmate.exception;

import lombok.Getter;

/**
 * ErrorCode를 담아 던지는 서비스 공통 예외입니다.
 *
 * TODO: GlobalExceptionHandler(@RestControllerAdvice)를 추가해
 *       이 예외를 ErrorDto 응답으로 변환하는 처리를 붙일 것.
 */
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
