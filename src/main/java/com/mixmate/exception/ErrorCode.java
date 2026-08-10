package com.mixmate.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 서비스 전역에서 사용하는 에러 코드입니다.
 *
 * 상수 이름이 곧 API 응답의 code 값입니다(name() 사용).
 * message는 기본 문구이며, API별로 다른 문구가 필요한 경우 호출부에서 덮어씁니다.
 */
@Getter
public enum ErrorCode {

    // 공통 (전 엔드포인트)
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생하였습니다."),

    // 인증 / 토큰
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    BLACKLIST_TOKEN(HttpStatus.UNAUTHORIZED, "로그아웃 처리된 토큰입니다."),
    JWT_TOKEN_PARSING_ERROR(HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않거나 만료되었습니다."),
    EMAIL_CONFLICTED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증이 완료되지 않았습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),

    // 그룹 / 참가자
    INVALID_INVITE_CODE(HttpStatus.NOT_FOUND, "유효하지 않은 초대코드입니다."),
    ALREADY_JOINED(HttpStatus.CONFLICT, "이미 참여중인 그룹입니다."),
    GROUP_LOCKED(HttpStatus.CONFLICT, "조 편성이 완료되어 인원 변경이 불가능합니다."),
    NOT_GROUP_ADMIN(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다."),
    INVALID_GROUP_STATUS(HttpStatus.CONFLICT, "현재 진행 상태에서 불가능한 요청입니다."),
    PROFILE_REQUIRED(HttpStatus.BAD_REQUEST, "추가정보를 먼저 입력해야 합니다."),
    ROUND_MISMATCH(HttpStatus.CONFLICT, "이미 종료되었거나 아직 시작되지 않은 라운드입니다."),

    // 조 편성 / 투표
    // TODO: 아래 2개는 명세 에러 코드 표에 HTTP 매핑이 없어 잠정값. team/vote 담당자 확인 후 정정할 것.
    INSUFFICIENT_PARTICIPANTS(HttpStatus.CONFLICT, "조 편성에 필요한 최소 인원을 충족하지 못했습니다."),
    VOTE_NOT_IN_PROGRESS(HttpStatus.CONFLICT, "투표가 진행중이 아닙니다."),
    ALREADY_VOTED(HttpStatus.CONFLICT, "이미 투표를 완료했습니다."),
    VOTE_TARGET_NOT_IN_TEAM(HttpStatus.CONFLICT, "같은 조원에게만 투표할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
