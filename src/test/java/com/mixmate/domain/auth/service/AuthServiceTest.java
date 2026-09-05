package com.mixmate.domain.auth.service;

import com.mixmate.domain.auth.dto.request.WithdrawReqDto;
import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.auth.repository.UserRepository;
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
    private RedisService redisService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private TokenService tokenService;

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
}
