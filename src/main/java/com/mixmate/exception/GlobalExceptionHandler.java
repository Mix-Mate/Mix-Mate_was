package com.mixmate.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 컨트롤러에서 빠져나온 예외를 명세서의 공통 에러 응답 포맷으로 변환합니다.
 *
 * 잘못된 입력이 400으로 나가는 경로는 다섯 가지입니다. 본문 필드 검증 실패, 경로 변수의 타입 불일치,
 * 필수 쿼리 파라미터 누락, 본문을 읽다 실패한 경우, 쿼리 파라미터가 제약에 걸린 경우이며
 * 다섯 다 INVALID_PARAMETER로 모읍니다. 하나라도 빠지면 400 응답 형식이 갈리므로 함께 처리합니다.
 *
 * 인증 실패(401)와 권한 없음(403)은 시큐리티 필터 단계에서 끝나므로 이곳을 거치지 않고,
 * CustomAuthenticationEntryPoint / CustomAccessDeniedHandler가 처리합니다.
 *
 * TODO: 미처리 예외(500) 핸들러는 필요해지는 시점에 추가할 것.
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

    /**
     * 본문 필드가 @Valid 검증에 실패한 경우. DTO에 적어둔 message가 그대로 errors에 실립니다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        // 한 필드에 검증이 여러 개 붙어 있으면(예: password의 @NotBlank + @Size) 키가 겹친다.
        // toMap은 키 충돌에 IllegalStateException을, 값이 null이면 NPE를 던져 400 대신 500이 나가므로
        // 병합 함수로 먼저 걸린 것을 남기고 메시지가 없는 경우도 막는다.
        Map<String, String> errors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> Objects.requireNonNullElse(
                                error.getDefaultMessage(), ErrorCode.INVALID_PARAMETER.getMessage()),
                        (first, second) -> first,
                        LinkedHashMap::new));

        return badRequest(ErrorCode.INVALID_PARAMETER.getMessage(), errors);
    }

    /**
     * 경로 변수나 쿼리 파라미터가 선언된 타입으로 변환되지 않는 경우. round 값의 오타가 대부분입니다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDto> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String reason = "'%s'은(는) 허용되지 않는 값입니다.".formatted(e.getValue());
        return badRequest(reason, Map.of(e.getName(), reason));
    }

    /**
     * 필수 쿼리 파라미터가 아예 오지 않은 경우. 참가자 목록의 round처럼 값이 없으면 요청을 해석할 수 없습니다.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorDto> handleMissingParameter(MissingServletRequestParameterException e) {
        String reason = "필수 항목입니다.";
        return badRequest("%s: %s".formatted(e.getParameterName(), reason),
                Map.of(e.getParameterName(), reason));
    }

    /**
     * 본문을 객체로 읽지 못한 경우. 본문 안의 enum 오타가 대부분이고, 깨진 JSON도 여기로 옵니다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDto> handleNotReadable(HttpMessageNotReadableException e) {
        // 원본 메시지에는 매핑하려던 내부 클래스 경로가 들어 있어 그대로 내보내면 안 된다.
        // 값 형식이 틀린 경우에만 필드와 값을 짚어주고, 그 외에는 본문 전체가 잘못됐다고만 알린다.
        if (e.getCause() instanceof InvalidFormatException cause) {
            String field = cause.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("."));

            if (!field.isEmpty()) {
                String reason = "'%s'은(는) 허용되지 않는 값입니다.".formatted(cause.getValue());
                return badRequest(reason, Map.of(field, reason));
            }
        }
        return badRequest("요청 본문을 읽을 수 없습니다.", null);
    }

    /**
     * 컨트롤러 파라미터에 붙인 제약에 걸린 경우. @Validated가 있는 컨트롤러에서만 발생합니다.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorDto> handleConstraintViolation(ConstraintViolationException e) {
        // propertyPath가 "메서드명.파라미터명"으로 오므로 마지막 마디만 남겨 본문 검증과 같은 모양으로 맞춘다.
        Map<String, String> errors = e.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> lastNodeOf(violation.getPropertyPath()),
                        violation -> Objects.requireNonNullElse(
                                violation.getMessage(), ErrorCode.INVALID_PARAMETER.getMessage()),
                        (first, second) -> first,
                        LinkedHashMap::new));

        return badRequest(ErrorCode.INVALID_PARAMETER.getMessage(), errors);
    }

    private String lastNodeOf(Path propertyPath) {
        String path = propertyPath.toString();
        return path.substring(path.lastIndexOf('.') + 1);
    }

    private ResponseEntity<ErrorDto> badRequest(String message, Map<String, String> errors) {
        return ResponseEntity
                .status(ErrorCode.INVALID_PARAMETER.getHttpStatus())
                .body(new ErrorDto(ErrorCode.INVALID_PARAMETER, message, errors));
    }
}
