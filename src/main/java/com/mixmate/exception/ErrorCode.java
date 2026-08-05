package com.mixmate.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 서비스 전역에서 사용하는 에러 코드입니다.
 *
 * TODO: 명세서 'enum 정리' 페이지가 확정되면 값 전체를 이곳으로 옮길 것.
 *       현재는 기존 코드 컴파일에 필요한 최소 항목만 정의되어 있습니다.
 */
@Getter
public enum ErrorCode {

    // 인증 / 사용자
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증 정보가 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // 토큰
    BLACKLIST_TOKEN(HttpStatus.UNAUTHORIZED, "로그아웃 처리된 토큰입니다."),
    JWT_TOKEN_PARSING_ERROR(HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않거나 만료되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
