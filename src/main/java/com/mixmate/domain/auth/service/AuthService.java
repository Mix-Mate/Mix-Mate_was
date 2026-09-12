package com.mixmate.domain.auth.service;

import com.mixmate.domain.auth.client.GoogleApiClient;
import com.mixmate.domain.auth.client.GoogleUserInfo;
import com.mixmate.domain.auth.client.KakaoApiClient;
import com.mixmate.domain.auth.client.KakaoUserInfo;
import com.mixmate.domain.auth.dto.request.GoogleLoginReqDto;
import com.mixmate.domain.auth.dto.request.KakaoLoginReqDto;
import com.mixmate.domain.auth.dto.request.LoginReqDto;
import com.mixmate.domain.auth.dto.request.PasswordResetReqDto;
import com.mixmate.domain.auth.dto.request.SignupReqDto;
import com.mixmate.domain.auth.dto.request.UserNameUpdateReqDto;
import com.mixmate.domain.auth.dto.request.WithdrawReqDto;
import com.mixmate.domain.auth.dto.response.LoginResDto;
import com.mixmate.domain.auth.dto.response.TokenReissueResDto;
import com.mixmate.domain.auth.entity.AuthProvider;
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
import org.springframework.util.StringUtils;

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
    private final KakaoApiClient kakaoApiClient;
    private final GoogleApiClient googleApiClient;

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
        // 1. 사용자 조회 (탈퇴한 계정은 존재하지 않는 것과 동일하게 처리)
        User user = userRepository.findByEmailAndDeletedAtIsNull(dto.getEmail())
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
        User user = userRepository.findByEmailAndDeletedAtIsNull(dto.getEmail())
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

        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
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

    /**
     * 회원 탈퇴 서비스 (소프트 딜리트)
     *
     * User 행을 지우지 않고 deletedAt만 찍는다. 참가·투표 등 이미 쌓인 데이터는 그대로 남기고,
     * 로그인·JWT 인증·비밀번호 재설정·토큰 재발급을 전부 막아 계정을 더 쓸 수 없게 만드는 방식이다.
     * 현재 기기의 리프레시 토큰을 지우고 액세스 토큰을 블랙리스트에 등록해, 다른 기기의 세션도 함께 끊는다.
     *
     * @param accessToken 현재 액세스 토큰 (본인 확인 및 세션 종료용)
     * @param dto         본인 확인용 비밀번호
     */
    @Transactional
    public void withdraw(String accessToken, WithdrawReqDto dto) {
        if (accessToken == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        String email;
        try {
            email = jwtUtil.getEmailFromToken(accessToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 소셜 로그인 계정은 비밀번호가 없으므로, 여기까지 도달했다는 것(=JWT 인증 통과) 자체로 본인 확인을 대신한다.
        if (user.isLocal()
                && (!StringUtils.hasText(dto.getPassword()) || !passwordEncoder.matches(dto.getPassword(), user.getPassword()))) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        user.withdraw();
        tokenService.deleteRefreshToken(user.getUserId());

        long remainingTime = jwtUtil.getExpiration(accessToken);
        redisService.setDataExpire(BLACKLIST_PREFIX + accessToken, "withdraw", remainingTime);
    }

    /**
     * 카카오 로그인 서비스
     *
     * 인가 코드를 카카오 서버와 주고받아 사용자 정보를 얻고, 처음 로그인하는 사용자면 자동으로
     * 회원가입시킨다. 발급하는 토큰의 형태(subject=email)와 응답 형태는 일반 로그인과 동일해서,
     * 프론트는 로그인 성공 이후 처리를 일반 로그인과 구분할 필요가 없다.
     *
     * @param dto 프론트가 카카오로부터 받은 인가 코드
     * @return 발급된 토큰과 사용자 정보
     */
    @Transactional
    public LoginResDto kakaoLogin(KakaoLoginReqDto dto) {
        KakaoUserInfo info = kakaoApiClient.getUserInfo(dto.getCode());

        User user = userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, info.providerId())
                .orElseGet(() -> registerKakaoUser(info));

        String accessToken = jwtUtil.createAccessToken(user.getEmail());
        String refreshToken = jwtUtil.createRefreshToken(user.getEmail());
        tokenService.saveRefreshToken(user.getUserId(), refreshToken);

        return LoginResDto.fromEntity(user, accessToken, refreshToken);
    }

    /**
     * 구글 로그인 서비스
     *
     * 인가 코드를 구글 서버와 주고받아 사용자 정보를 얻고, 처음 로그인하는 사용자면 자동으로 회원가입시킨다.
     * 발급 토큰과 응답 형태는 일반 로그인·카카오 로그인과 동일하다.
     *
     * @param dto 프론트가 구글로부터 받은 인가 코드
     * @return 발급된 토큰과 사용자 정보
     */
    @Transactional
    public LoginResDto googleLogin(GoogleLoginReqDto dto) {
        GoogleUserInfo info = googleApiClient.getUserInfo(dto.getCode());

        User user = userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, info.providerId())
                .orElseGet(() -> registerGoogleUser(info));

        String accessToken = jwtUtil.createAccessToken(user.getEmail());
        String refreshToken = jwtUtil.createRefreshToken(user.getEmail());
        tokenService.saveRefreshToken(user.getUserId(), refreshToken);

        return LoginResDto.fromEntity(user, accessToken, refreshToken);
    }

    private User registerKakaoUser(KakaoUserInfo info) {
        if (!StringUtils.hasText(info.email())) {
            // 카카오 이메일 동의항목이 선택 동의로 설정되어 있거나, 비즈 앱 미전환 상태라 이메일을
            // 못 받아온 경우. 이메일을 JWT subject이자 유니크 키로 쓰는 지금 구조상 필수로 요구한다.
            throw new CustomException(ErrorCode.OAUTH_EMAIL_CONSENT_REQUIRED);
        }
        if (userRepository.existsByEmail(info.email())) {
            // 이미 일반 회원가입으로 존재하는 이메일이면 자동으로 연동하지 않고 막는다 — 이메일이
            // 같다고 같은 사람이라고 가정하는 대신, 기존 방법으로 로그인하도록 유도한다.
            throw new CustomException(ErrorCode.EMAIL_CONFLICTED, "이미 다른 방식으로 가입된 이메일입니다. 기존 방법으로 로그인해주세요.");
        }

        String nickname = StringUtils.hasText(info.nickname()) ? info.nickname() : "카카오사용자";
        String userName = nickname.length() > 10 ? nickname.substring(0, 10) : nickname;

        return userRepository.save(User.ofKakao(info.email(), userName, info.providerId()));
    }

    private User registerGoogleUser(GoogleUserInfo info) {
        if (!StringUtils.hasText(info.email())) {
            // 프론트가 인증 URL의 scope에 email을 빠뜨린 경우. 이메일을 JWT subject이자
            // 유니크 키로 쓰는 지금 구조상 필수로 요구한다.
            throw new CustomException(ErrorCode.OAUTH_EMAIL_CONSENT_REQUIRED, "구글 계정에서 이메일 제공에 동의해야 합니다.");
        }
        if (userRepository.existsByEmail(info.email())) {
            // 이미 일반 회원가입이나 카카오로 존재하는 이메일이면 자동으로 연동하지 않고 막는다 — 이메일이
            // 같다고 같은 사람이라고 가정하는 대신, 기존 방법으로 로그인하도록 유도한다.
            throw new CustomException(ErrorCode.EMAIL_CONFLICTED, "이미 다른 방식으로 가입된 이메일입니다. 기존 방법으로 로그인해주세요.");
        }

        String name = StringUtils.hasText(info.name()) ? info.name() : "구글사용자";
        String userName = name.length() > 10 ? name.substring(0, 10) : name;

        return userRepository.save(User.ofGoogle(info.email(), userName, info.providerId()));
    }

    /**
     * 마이페이지 이름 수정 서비스
     *
     * @param userId 로그인한 사용자 식별자
     * @param dto    새 이름
     */
    @Transactional
    public void updateUserName(Long userId, UserNameUpdateReqDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateUserName(dto.getUserName());
    }
}
