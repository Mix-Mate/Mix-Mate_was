package com.mixmate.domain.auth.service;

import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.auth.repository.UserRepository;
import com.mixmate.exception.CustomException;
import com.mixmate.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetEmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PasswordResetEmailService passwordResetEmailService;

    private User localUser;
    private User socialUser;

    @BeforeEach
    void setUp() {
        localUser = User.builder()
                .userId(1L)
                .userName("김대현")
                .email("local@example.com")
                .password("encoded-password")
                .build();

        socialUser = User.ofKakao("social@example.com", "카카오유저", "kakao-id");
    }

    @Test
    @DisplayName("가입되지 않은 이메일이면 인증번호를 보내지 않고 404를 던진다")
    void sendVerificationCodeFailsWhenUserNotFound() {
        when(userRepository.findByEmailAndDeletedAtIsNull("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetEmailService.sendVerificationCode("unknown@example.com"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        Mockito.verifyNoInteractions(javaMailSender, stringRedisTemplate);
    }

    @Test
    @DisplayName("소셜 로그인 계정이면 인증 메일을 보내지 않고 NOT_LOCAL_ACCOUNT를 던진다")
    void sendVerificationCodeFailsWhenSocialAccount() {
        when(userRepository.findByEmailAndDeletedAtIsNull("social@example.com")).thenReturn(Optional.of(socialUser));

        assertThatThrownBy(() -> passwordResetEmailService.sendVerificationCode("social@example.com"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_LOCAL_ACCOUNT);

        Mockito.verifyNoInteractions(javaMailSender, stringRedisTemplate);
    }

    @Test
    @DisplayName("로컬 계정이면 인증 메일을 보내고 Redis에 인증번호를 5분간 저장한다")
    void sendVerificationCodeSucceedsForLocalAccount() {
        when(userRepository.findByEmailAndDeletedAtIsNull("local@example.com")).thenReturn(Optional.of(localUser));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        passwordResetEmailService.sendVerificationCode("local@example.com");

        verify(javaMailSender).send(any(org.springframework.mail.SimpleMailMessage.class));
        verify(valueOperations).set(eq("PW_RESET_AUTH:local@example.com"), anyString(), eq(5L), eq(TimeUnit.MINUTES));
    }
}
