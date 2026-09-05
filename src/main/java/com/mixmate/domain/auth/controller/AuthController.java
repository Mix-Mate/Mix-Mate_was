package com.mixmate.domain.auth.controller;

import com.mixmate.domain.auth.api.AuthApi;
import com.mixmate.domain.auth.dto.request.LoginReqDto;
import com.mixmate.domain.auth.dto.request.PasswordResetReqDto;
import com.mixmate.domain.auth.dto.request.SignupReqDto;
import com.mixmate.domain.auth.dto.request.TokenReissueReqDto;
import com.mixmate.domain.auth.dto.request.WithdrawReqDto;
import com.mixmate.domain.auth.dto.response.LoginResDto;
import com.mixmate.domain.auth.dto.response.TokenReissueResDto;
import com.mixmate.domain.auth.service.AuthService;
import com.mixmate.domain.auth.service.PasswordResetEmailService;
import com.mixmate.domain.auth.service.SignUpEmailService;
import com.mixmate.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController implements AuthApi {
    private final AuthService authService;
    private final SignUpEmailService signUpEmailService;
    private final PasswordResetEmailService passwordResetEmailService;
    private final JwtUtil jwtUtil;

    // 회원가입 API
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupReqDto signupReqDto) {
        authService.signUp(signupReqDto);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    // 인증번호 발송
    @PostMapping("/send")
    public ResponseEntity<String> sendCode(@RequestParam String email) {
        signUpEmailService.sendVerificationCode(email);
        return ResponseEntity.ok("인증 번호가 발송되었습니다.");
    }

    // 인증번호 검증
    @PostMapping("/verify")
    public ResponseEntity<String> verifyCode(@RequestParam String email, @RequestParam String code) {
        boolean isVerified = signUpEmailService.verifyCode(email, code);
        if (isVerified) {
            return ResponseEntity.ok("인증 성공!");
        }
        return ResponseEntity.badRequest().body("인증 번호가 틀렸거나 만료되었습니다.");
    }

    // 비밀번호 재설정 인증번호 발송
    @PostMapping("/password/send")
    public ResponseEntity<String> sendPasswordResetCode(@RequestParam String email) {
        passwordResetEmailService.sendVerificationCode(email);
        return ResponseEntity.ok("인증 번호가 발송되었습니다.");
    }

    // 비밀번호 재설정 인증번호 검증
    @PostMapping("/password/verify")
    public ResponseEntity<String> verifyPasswordResetCode(@RequestParam String email, @RequestParam String code) {
        boolean isVerified = passwordResetEmailService.verifyCode(email, code);
        if (isVerified) {
            return ResponseEntity.ok("인증 성공!");
        }
        return ResponseEntity.badRequest().body("인증 번호가 틀렸거나 만료되었습니다.");
    }

    // 비밀번호 재설정
    @PostMapping("/password/reset")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody PasswordResetReqDto passwordResetReqDto) {
        authService.resetPassword(passwordResetReqDto);
        return ResponseEntity.ok("비밀번호가 변경되었습니다.");
    }

    // 로그인 API
    @PostMapping("/login")
    public ResponseEntity<LoginResDto> login(@Valid @RequestBody LoginReqDto loginReqDto) {
        // 토큰과 유저 정보 가져오기
        LoginResDto loginRes = authService.login(loginReqDto);

        // 쿠키 만료시간을 실제 토큰 만료시간(JwtUtil)과 맞춘다.
        ResponseCookie accessCookie = createCookie("accessToken", loginRes.getAccessToken(), jwtUtil.getAccessValid());
        ResponseCookie refreshCookie = createCookie("refreshToken", loginRes.getRefreshToken(), jwtUtil.getRefreshValid());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(loginRes);
    }

    // accessToken 재발급
    @PostMapping("/reissue")
    public ResponseEntity<TokenReissueResDto> reissue(
            @RequestBody(required = false) TokenReissueReqDto reissueReqDto,
            HttpServletRequest request
    ) {
        String refreshToken = jwtUtil.extractRefreshTokenFromCookie(request);
        if (refreshToken == null && reissueReqDto != null) {
            refreshToken = reissueReqDto.getRefreshToken();
        }

        TokenReissueResDto result = authService.reissueAccessToken(refreshToken);

        ResponseCookie accessCookie = createCookie("accessToken", result.getAccessToken(), jwtUtil.getAccessValid());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .body(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        // 1. 요청의 쿠키에서 액세스 토큰 꺼내기
        String token = extractCookie(request, "accessToken");

        // 2. 서비스 로직 실행 (DB 리프레시 토큰 삭제 + 레디스 블랙리스트 등록)
        if (token != null) {
            authService.logout(token);
        }

        // 3. 클라이언트 쿠키 삭제 (Max-Age를 0으로 설정, 로그인 때 심은 두 쿠키 모두 제거)
        ResponseCookie expiredAccessCookie = createCookie("accessToken", "", 0);
        ResponseCookie expiredRefreshCookie = createCookie("refreshToken", "", 0);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredAccessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie.toString())
                .body("성공적으로 로그아웃되었습니다.");
    }

    // 회원 탈퇴 API (소프트 딜리트)
    @DeleteMapping("/withdraw")
    public ResponseEntity<String> withdraw(@Valid @RequestBody WithdrawReqDto withdrawReqDto, HttpServletRequest request) {
        String token = extractCookie(request, "accessToken");
        authService.withdraw(token, withdrawReqDto);

        ResponseCookie expiredAccessCookie = createCookie("accessToken", "", 0);
        ResponseCookie expiredRefreshCookie = createCookie("refreshToken", "", 0);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredAccessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie.toString())
                .body("회원 탈퇴가 완료되었습니다.");
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private ResponseCookie createCookie(String name, String value, long maxAgeMillis) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(TimeUnit.MILLISECONDS.toSeconds(maxAgeMillis))
                .build();
    }
}
