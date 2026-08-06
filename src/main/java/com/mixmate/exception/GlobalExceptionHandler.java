package com.mixmate.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 컨트롤러에서 빠져나온 예외를 명세서의 공통 에러 응답 포맷으로 변환합니다.
 *
 * 인증 실패(401)와 권한 없음(403)은 시큐리티 필터 단계에서 끝나므로 이곳을 거치지 않고,
 * CustomAuthenticationEntryPoint / CustomAccessDeniedHandler가 처리합니다.
 *
 * TODO: @Valid 검증 실패와 미처리 예외(500) 핸들러는 필요해지는 시점에 추가할 것.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorDto> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(new ErrorDto(errorCode, e.getMessage()));
    }
}
