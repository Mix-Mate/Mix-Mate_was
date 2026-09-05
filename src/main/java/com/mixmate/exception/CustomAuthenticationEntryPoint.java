package com.mixmate.exception;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        // JwtAuthenticationFilter가 블랙리스트·만료 등 구체적인 사유를 request attribute에 미리 담아둔다.
        // 없으면(토큰 자체가 없는 경우 등) 기본값인 UNAUTHORIZED로 처리한다.
        boolean hasSpecificReason = request.getAttribute("exception") instanceof ErrorCode;
        ErrorCode errorCode = hasSpecificReason
                ? (ErrorCode) request.getAttribute("exception")
                : ErrorCode.UNAUTHORIZED;
        String message = hasSpecificReason ? errorCode.getMessage() : "토큰이 없거나 만료되었습니다.";

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorDto errorDto = new ErrorDto(errorCode, message);

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(errorDto));
    }
}
