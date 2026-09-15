package com.mixmate.domain.auth.service;

import com.mixmate.domain.auth.client.KakaoApiClient;
import com.mixmate.domain.auth.client.KakaoUserInfo;
import com.mixmate.domain.auth.dto.request.KakaoLoginReqDto;
import com.mixmate.domain.auth.dto.request.LoginReqDto;
import com.mixmate.domain.auth.dto.request.PasswordResetReqDto;
import com.mixmate.domain.auth.dto.request.UserNameUpdateReqDto;
import com.mixmate.domain.auth.dto.request.WithdrawReqDto;
import com.mixmate.domain.auth.dto.response.LoginResDto;
import com.mixmate.domain.auth.dto.response.MyInfoResDto;
import com.mixmate.domain.auth.dto.response.TokenReissueResDto;
import com.mixmate.domain.auth.entity.AuthProvider;
import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.auth.repository.UserRepository;
import com.mixmate.domain.participant.repository.ParticipantRepository;
import com.mixmate.exception.CustomException;
import com.mixmate.exception.ErrorCode;
import com.mixmate.redis.RedisService;
import com.mixmate.security.JwtUtil;
import com.mixmate.security.TokenService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private RedisService redisService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private TokenService tokenService;
    @Mock
    private KakaoApiClient kakaoApiClient;

    @InjectMocks
    private AuthService authService;

    private User activeUser;
    private WithdrawReqDto withdrawReqDto;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .userId(1L)
                .userName("김대현")
                .email("kdh@example.com")
                .password("encoded-password")
                .build();

        withdrawReqDto = WithdrawReqDto.builder()
                .password("raw-password")
                .build();
    }

    @Test
    @DisplayName("액세스 토큰이 없으면 로그인이 필요하다는 에러를 던진다")
    void withdrawFailsWhenTokenMissing() {
        assertThatThrownBy(() -> authService.withdraw(null, withdrawReqDto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        Mockito.verifyNoInteractions(userRepository, tokenService, redisService);
    }

    @Test
    @DisplayName("토큰 파싱에 실패하면 로그인이 필요하다는 에러를 던진다")
    void withdrawFailsWhenTokenInvalid() {
        when(jwtUtil.getEmailFromToken("bad-token")).thenThrow(new JwtException("만료됨"));

        assertThatThrownBy(() -> authService.withdraw("bad-token", withdrawReqDto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("이미 탈퇴했거나 존재하지 않는 계정이면 404를 던진다")
    void withdrawFailsWhenUserNotFound() {
        when(jwtUtil.getEmailFromToken("token")).thenReturn("kdh@example.com");
        when(userRepository.findByEmailAndDeletedAtIsNull("kdh@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.withdraw("token", withdrawReqDto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("비밀번호가 틀리면 탈퇴되지 않는다")
    void withdrawFailsWhenPasswordMismatch() {
        when(jwtUtil.getEmailFromToken("token")).thenReturn("kdh@example.com");
        when(userRepository.findByEmailAndDeletedAtIsNull("kdh@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("raw-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.withdraw("token", withdrawReqDto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PASSWORD);

        assertThat(activeUser.getDeletedAt()).isNull();
        verify(tokenService, never()).deleteRefreshToken(anyLong());
    }

    @Test
    @DisplayName("본인 확인에 성공하면 소프트 딜리트되고, 모든 기기의 세션이 끊긴다")
    void withdrawSucceeds() {
        when(jwtUtil.getEmailFromToken("token")).thenReturn("kdh@example.com");
        when(userRepository.findByEmailAndDeletedAtIsNull("kdh@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("raw-password", "encoded-password")).thenReturn(true);
        when(jwtUtil.getExpiration("token")).thenReturn(1_800_000L);

        authService.withdraw("token", withdrawReqDto);

        // 탈퇴 시각이 찍히고, 원래 이메일은 반납되어 재가입에 쓸 수 있게 값 자체가 바뀐다.
        assertThat(activeUser.getDeletedAt()).isNotNull();
        assertThat(activeUser.getEmail()).isNotEqualTo("kdh@example.com");

        // 현재 기기뿐 아니라 다른 기기 세션까지 끊기도록 리프레시 토큰을 지운다.
        verify(tokenService).deleteRefreshToken(1L);
        // 아직 만료 전인 현재 액세스 토큰을 블랙리스트에 등록해 즉시 무효화한다.
        verify(redisService).setDataExpire(eq("BLACKLIST:token"), anyString(), eq(1_800_000L));
    }

    // ---------- login ----------

    @Test
    @DisplayName("이메일/비밀번호가 맞으면 로그인에 성공하고 토큰이 발급된다")
    void loginSucceeds() {
        LoginReqDto dto = LoginReqDto.builder().email("kdh@example.com").password("raw-password").build();
        when(userRepository.findByEmailAndDeletedAtIsNull("kdh@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("raw-password", "encoded-password")).thenReturn(true);
        when(jwtUtil.createAccessToken("kdh@example.com")).thenReturn("access-token");
        when(jwtUtil.createRefreshToken("kdh@example.com")).thenReturn("refresh-token");

        LoginResDto result = authService.login(dto);

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(result.getProvider()).isEqualTo("LOCAL");
        verify(tokenService).saveRefreshToken(1L, "refresh-token");
    }

    @Test
    @DisplayName("존재하지 않는(또는 탈퇴한) 계정이면 로그인이 실패한다")
    void loginFailsWhenUserNotFound() {
        LoginReqDto dto = LoginReqDto.builder().email("nope@example.com").password("raw-password").build();
        when(userRepository.findByEmailAndDeletedAtIsNull("nope@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("비밀번호가 틀리면 로그인이 실패한다")
    void loginFailsWhenPasswordMismatch() {
        LoginReqDto dto = LoginReqDto.builder().email("kdh@example.com").password("wrong-password").build();
        when(userRepository.findByEmailAndDeletedAtIsNull("kdh@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PASSWORD);
    }

    // ---------- resetPassword ----------

    @Test
    @DisplayName("이메일 인증이 완료된 로컬 계정은 비밀번호를 재설정할 수 있다")
    void resetPasswordSucceeds() {
        PasswordResetReqDto dto = PasswordResetReqDto.builder()
                .email("kdh@example.com").newPassword("newPassword123").build();
        when(userRepository.findByEmailAndDeletedAtIsNull("kdh@example.com")).thenReturn(Optional.of(activeUser));
        when(redisService.getData("PW_RESET_VERIFIED:kdh@example.com")).thenReturn("DONE");
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-encoded-password");

        authService.resetPassword(dto);

        assertThat(activeUser.getPassword()).isEqualTo("new-encoded-password");
        verify(redisService).deleteData("PW_RESET_VERIFIED:kdh@example.com");
        verify(tokenService).deleteRefreshToken(1L);
    }

    @Test
    @DisplayName("소셜 로그인 계정은 비밀번호 재설정이 막힌다")
    void resetPasswordFailsForSocialAccount() {
        User kakaoUser = User.builder()
                .userId(2L).userName("카카오유저").email("kakao@example.com")
                .provider(AuthProvider.KAKAO).providerId("12345").build();
        PasswordResetReqDto dto = PasswordResetReqDto.builder()
                .email("kakao@example.com").newPassword("newPassword123").build();
        when(userRepository.findByEmailAndDeletedAtIsNull("kakao@example.com")).thenReturn(Optional.of(kakaoUser));

        assertThatThrownBy(() -> authService.resetPassword(dto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_LOCAL_ACCOUNT);

        // provider 체크가 이메일 인증 여부 체크보다 먼저 일어나므로, redis는 아예 조회하지 않는다.
        Mockito.verifyNoInteractions(redisService);
    }

    @Test
    @DisplayName("이메일 인증이 안 됐으면 비밀번호를 재설정할 수 없다")
    void resetPasswordFailsWhenEmailNotVerified() {
        PasswordResetReqDto dto = PasswordResetReqDto.builder()
                .email("kdh@example.com").newPassword("newPassword123").build();
        when(userRepository.findByEmailAndDeletedAtIsNull("kdh@example.com")).thenReturn(Optional.of(activeUser));
        when(redisService.getData("PW_RESET_VERIFIED:kdh@example.com")).thenReturn(null);

        assertThatThrownBy(() -> authService.resetPassword(dto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);
    }

    // ---------- reissueAccessToken ----------

    @Test
    @DisplayName("리프레시 토큰이 없으면 재발급이 거부된다")
    void reissueFailsWhenRefreshTokenMissing() {
        assertThatThrownBy(() -> authService.reissueAccessToken(null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("리프레시 토큰이 Redis에 저장된 값과 같으면 새 액세스 토큰을 발급한다")
    void reissueSucceeds() {
        when(jwtUtil.getEmailFromToken("refresh-token")).thenReturn("kdh@example.com");
        when(userRepository.findByEmailAndDeletedAtIsNull("kdh@example.com")).thenReturn(Optional.of(activeUser));
        when(tokenService.getRefreshToken(1L)).thenReturn("refresh-token");
        when(jwtUtil.createAccessToken("kdh@example.com")).thenReturn("new-access-token");

        TokenReissueResDto result = authService.reissueAccessToken("refresh-token");

        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
    }

    @Test
    @DisplayName("Redis에 저장된 리프레시 토큰과 다르면(로그아웃 등) 재발급이 거부된다")
    void reissueFailsWhenTokenDoesNotMatchSaved() {
        when(jwtUtil.getEmailFromToken("refresh-token")).thenReturn("kdh@example.com");
        when(userRepository.findByEmailAndDeletedAtIsNull("kdh@example.com")).thenReturn(Optional.of(activeUser));
        when(tokenService.getRefreshToken(1L)).thenReturn("other-device-token");

        assertThatThrownBy(() -> authService.reissueAccessToken("refresh-token"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    // ---------- logout ----------

    @Test
    @DisplayName("로그아웃하면 리프레시 토큰이 삭제되고 액세스 토큰이 블랙리스트에 등록된다")
    void logoutSucceeds() {
        when(jwtUtil.getEmailFromToken("token")).thenReturn("kdh@example.com");
        when(userRepository.findByEmail("kdh@example.com")).thenReturn(Optional.of(activeUser));
        when(jwtUtil.getExpiration("token")).thenReturn(1_800_000L);

        authService.logout("token");

        verify(tokenService).deleteRefreshToken(1L);
        verify(redisService).setDataExpire(eq("BLACKLIST:token"), anyString(), eq(1_800_000L));
    }

    @Test
    @DisplayName("이미 손상되었거나 만료된 토큰으로 로그아웃해도 예외 없이 조용히 끝난다")
    void logoutIgnoresInvalidToken() {
        when(jwtUtil.getEmailFromToken("bad-token")).thenThrow(new JwtException("만료됨"));

        authService.logout("bad-token");

        Mockito.verifyNoInteractions(tokenService, redisService);
    }

    // ---------- updateUserName ----------

    @Test
    @DisplayName("로그인한 사용자의 이름을 수정한다")
    void updateUserNameSucceeds() {
        UserNameUpdateReqDto dto = UserNameUpdateReqDto.builder().userName("새이름").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        authService.updateUserName(1L, dto);

        assertThat(activeUser.getUserName()).isEqualTo("새이름");
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 이름은 수정할 수 없다")
    void updateUserNameFailsWhenUserNotFound() {
        UserNameUpdateReqDto dto = UserNameUpdateReqDto.builder().userName("새이름").build();
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.updateUserName(99L, dto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    // ---------- getMyInfo ----------

    @Test
    @DisplayName("로그인한 사용자의 현재 정보를 조회한다")
    void getMyInfoSucceeds() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(participantRepository.countByUserAndIsMvpTrue(activeUser)).thenReturn(3L);

        MyInfoResDto result = authService.getMyInfo(1L);

        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("kdh@example.com");
        assertThat(result.getUserName()).isEqualTo("김대현");
        assertThat(result.getProvider()).isEqualTo("LOCAL");
        assertThat(result.getMvpCount()).isEqualTo(3L);
    }

    // ---------- kakaoLogin ----------

    @Test
    @DisplayName("이미 가입된 카카오 계정이면 그대로 로그인시킨다")
    void kakaoLoginSucceedsForExistingUser() {
        User kakaoUser = User.builder()
                .userId(3L).userName("카카오유저").email("kakao@example.com")
                .provider(AuthProvider.KAKAO).providerId("kakao-id-1").build();
        KakaoLoginReqDto dto = KakaoLoginReqDto.builder().code("auth-code").build();
        when(kakaoApiClient.getUserInfo("auth-code"))
                .thenReturn(new KakaoUserInfo("kakao-id-1", "kakao@example.com", "카카오유저"));
        when(userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, "kakao-id-1"))
                .thenReturn(Optional.of(kakaoUser));
        when(jwtUtil.createAccessToken("kakao@example.com")).thenReturn("access-token");
        when(jwtUtil.createRefreshToken("kakao@example.com")).thenReturn("refresh-token");

        LoginResDto result = authService.kakaoLogin(dto);

        assertThat(result.getProvider()).isEqualTo("KAKAO");
        verify(userRepository, never()).save(Mockito.any());
    }

    @Test
    @DisplayName("처음 카카오로 로그인하면 자동으로 회원가입된다")
    void kakaoLoginRegistersNewUser() {
        KakaoLoginReqDto dto = KakaoLoginReqDto.builder().code("auth-code").build();
        when(kakaoApiClient.getUserInfo("auth-code"))
                .thenReturn(new KakaoUserInfo("kakao-id-2", "new-kakao@example.com", "새카카오유저"));
        when(userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, "kakao-id-2"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("new-kakao@example.com")).thenReturn(false);
        when(userRepository.save(Mockito.any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtUtil.createAccessToken("new-kakao@example.com")).thenReturn("access-token");
        when(jwtUtil.createRefreshToken("new-kakao@example.com")).thenReturn("refresh-token");

        LoginResDto result = authService.kakaoLogin(dto);

        assertThat(result.getEmail()).isEqualTo("new-kakao@example.com");
        assertThat(result.getProvider()).isEqualTo("KAKAO");
    }

    @Test
    @DisplayName("카카오 계정에 이메일 동의가 안 되어 있으면 로그인이 거부된다")
    void kakaoLoginFailsWhenEmailMissing() {
        KakaoLoginReqDto dto = KakaoLoginReqDto.builder().code("auth-code").build();
        when(kakaoApiClient.getUserInfo("auth-code"))
                .thenReturn(new KakaoUserInfo("kakao-id-3", null, "이메일없는유저"));
        when(userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, "kakao-id-3"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.kakaoLogin(dto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.OAUTH_EMAIL_CONSENT_REQUIRED);
    }

    @Test
    @DisplayName("이미 일반 회원가입으로 존재하는 이메일이면 카카오 자동가입을 막는다")
    void kakaoLoginFailsWhenEmailAlreadyLocal() {
        KakaoLoginReqDto dto = KakaoLoginReqDto.builder().code("auth-code").build();
        when(kakaoApiClient.getUserInfo("auth-code"))
                .thenReturn(new KakaoUserInfo("kakao-id-4", "kdh@example.com", "카카오유저"));
        when(userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, "kakao-id-4"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("kdh@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.kakaoLogin(dto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_CONFLICTED);
    }
}
