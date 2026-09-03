package com.mixmate.domain.auth.service;

import com.mixmate.domain.auth.dto.request.LoginReqDto;
import com.mixmate.domain.auth.dto.request.PasswordResetReqDto;
import com.mixmate.domain.auth.dto.request.SignupReqDto;
import com.mixmate.domain.auth.dto.response.LoginResDto;
import com.mixmate.domain.auth.dto.response.TokenReissueResDto;
import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.auth.repository.UserRepository;
import com.mixmate.exception.CustomException;
import com.mixmate.exception.ErrorCode;
import com.mixmate.redis.RedisService;
import com.mixmate.security.JwtUtil;
import com.mixmate.security.TokenService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입, 로그인, 로그아웃을 처리하는 서비스입니다.
 * 이메일 인증 발송/검증은 {@link SignUpEmailService}가 별도로 담당합니다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BLACKLIST_PREFIX = "BLACKLIST:";
    private static final String VERIFIED_EMAIL_PREFIX = "VERIFIED_EMAIL:";
    private static final String PW_RESET_VERIFIED_PREFIX = "PW_RESET_VERIFIED:";

    private final UserRepository userRepository;
    private final RedisService redisService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    /**
     * 회원가입 서비스
     *
     * SignUpEmailService.verifyCode에서 인증 성공 시 Redis에 "VERIFIED_EMAIL:" + email -> "DONE"으로
     * 저장해두는 것을 전제로 합니다.
     *
     * @param dto 회원가입 정보
     */
    @Transactional
    public void signUp(SignupReqDto dto) {
        // 1. 이메일 중복 체크
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_CONFLICTED);
        }

        // 2. 이메일 인증 완료 여부 체크
        String verifiedKey = VERIFIED_EMAIL_PREFIX + dto.getEmail();
        if (!"DONE".equals(redisService.getData(verifiedKey))) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        redisService.deleteData(verifiedKey);

        // 3. 회원 저장
        userRepository.save(dto.toEntity(passwordEncoder));
    }

    /**
     * 로그인 서비스
     *
     * @param dto 로그인 정보
     * @return 발급된 토큰과 사용자 정보
     */
    @Transactional
    public LoginResDto login(LoginReqDto dto) {
        // 1. 사용자 조회
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 비밀번호 확인
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 3. 토큰 발급 및 리프레시 토큰 저장
        String accessToken = jwtUtil.createAccessToken(user.getEmail());
        String refreshToken = jwtUtil.createRefreshToken(user.getEmail());
        tokenService.saveRefreshToken(user.getUserId(), refreshToken);

        return LoginResDto.fromEntity(user, accessToken, refreshToken);
    }

    /**
     * 비밀번호 재설정 서비스
     *
     * PasswordResetEmailService.verifyCode에서 인증 성공 시 Redis에 "PW_RESET_VERIFIED:" + email -> "DONE"으로
     * 저장해두는 것을 전제로 합니다. 재설정 후에는 다른 기기의 로그인을 무효화하기 위해 리프레시 토큰을 삭제합니다.
     *
     * @param dto 재설정할 이메일과 새 비밀번호
     */
    @Transactional
    public void resetPassword(PasswordResetReqDto dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String verifiedKey = PW_RESET_VERIFIED_PREFIX + dto.getEmail();
        if (!"DONE".equals(redisService.getData(verifiedKey))) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        redisService.deleteData(verifiedKey);

        user.updatePassword(passwordEncoder.encode(dto.getNewPassword()));
        tokenService.deleteRefreshToken(user.getUserId());
    }

    /**
     * accessToken 재발급 서비스
     *
     * 리프레시 토큰이 유효한 서명·만료시간을 가진 JWT인지 먼저 검증하고, 로그인 때 Redis에 저장해둔 값과
     * 같은지 대조한다. 로그아웃했거나 다른 기기에서 재로그인해 값이 바뀌었으면 여기서 걸린다.
     *
     * @param refreshToken 클라이언트가 보낸 리프레시 토큰 (쿠키 우선, 없으면 바디)
     * @return 새로 발급된 accessToken
     */
    @Transactional(readOnly = true)
    public TokenReissueResDto reissueAccessToken(String refreshToken) {
        if (refreshToken == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "리프레시 토큰이 없습니다.");
        }

        String email;
        try {
            email = jwtUtil.getEmailFromToken(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.JWT_TOKEN_PARSING_ERROR);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String savedRefreshToken = tokenService.getRefreshToken(user.getUserId());
        if (!refreshToken.equals(savedRefreshToken)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다.");
        }

        return TokenReissueResDto.of(jwtUtil.createAccessToken(email));
    }

    /**
     * 로그아웃 서비스
     *
     * 리프레시 토큰을 삭제하고, 남은 유효시간만큼 액세스 토큰을 블랙리스트에 등록합니다.
     * 이미 만료되었거나 손상된 토큰은 어차피 사용할 수 없으므로, 파싱에 실패해도 예외를 던지지 않고 조용히 종료합니다.
     *
     * @param accessToken 현재 액세스 토큰
     */
    @Transactional
    public void logout(String accessToken) {
        String email;
        try {
            email = jwtUtil.getEmailFromToken(accessToken);
        } catch (JwtException | IllegalArgumentException e) {
            return;
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        tokenService.deleteRefreshToken(user.getUserId());

        long remainingTime = jwtUtil.getExpiration(accessToken);
        redisService.setDataExpire(BLACKLIST_PREFIX + accessToken, "logout", remainingTime);
    }
}
