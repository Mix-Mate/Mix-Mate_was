package com.mixmate.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.util.Map;

/**
 * 명세서 '공통 에러 응답 포맷'에 대응하는 에러 응답 body입니다.
 * code는 ErrorCode 이름에서만 나와야 하므로 생성자에서 String이 아닌 ErrorCode를 받습니다.
 *
 * errors는 입력값이 잘못된 400에만 실립니다. 나머지 응답에서는 null이라 키 자체가 나가지 않습니다.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDto {

    private final String code;

    private final String message;

    private final Map<String, String> errors;

    public ErrorDto(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public ErrorDto(ErrorCode errorCode, String message, Map<String, String> errors) {
        this.code = errorCode.name();
        this.message = message;
        this.errors = errors;
    }
}
