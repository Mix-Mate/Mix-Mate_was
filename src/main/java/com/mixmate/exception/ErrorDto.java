package com.mixmate.exception;

import lombok.Getter;

/**
 * 명세서 '공통 에러 응답 포맷'에 대응하는 에러 응답 body입니다.
 * code는 ErrorCode 이름에서만 나와야 하므로 생성자에서 String이 아닌 ErrorCode를 받습니다.
 *
 * TODO: 400 INVALID_PARAMETER는 errors 배열을 함께 내려줘야 합니다(List<{field, reason}> 형태).
 *       @Valid를 붙이는 시점에 @JsonInclude(NON_NULL)과 같이 추가할 것.
 */
@Getter
public class ErrorDto {

    private final String code;
    private final String message;

    public ErrorDto(ErrorCode errorCode, String message) {
        this.code = errorCode.name();
        this.message = message;
    }
}
