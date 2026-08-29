package com.mixmate.domain.auth.service;

import com.mixmate.domain.auth.repository.UserRepository;
import com.mixmate.exception.CustomException;
import com.mixmate.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 비밀번호 재설정용 이메일 인증번호를 발송하고 검증하는 서비스입니다.
 * 가입된 이메일인지 먼저 확인한다는 점과 Redis 키 네임스페이스가 다르다는 점을 빼면
 * {@link SignUpEmailService}와 동일한 패턴입니다.
 * 인증에 성공하면 Redis에 "PW_RESET_VERIFIED:" + email -> "DONE"을 남겨, {@link AuthService#resetPassword}가
 * 그 상태를 보고 비밀번호 변경을 허용합니다.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetEmailService {

    private static final String PW_RESET_AUTH_PREFIX = "PW_RESET_AUTH:";
    private static final String PW_RESET_VERIFIED_PREFIX = "PW_RESET_VERIFIED:";

    private final JavaMailSender javaMailSender;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserRepository userRepository;

    /**
     * 가입된 이메일로 6자리 인증 번호를 발송하고, 해당 번호를 Redis에 5분간 저장합니다.
     *
     * @param email 인증 번호를 수신할 사용자의 이메일 주소
     * @throws CustomException 가입되지 않은 이메일인 경우 (USER_NOT_FOUND)
     */
    public void sendVerificationCode(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        String code = generateCode();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[Mixmate] 비밀번호 재설정 인증 번호입니다.");
        message.setText("인증 번호: " + code + "\n5분 이내에 입력해주세요.");

        javaMailSender.send(message);

        stringRedisTemplate.opsForValue().set(PW_RESET_AUTH_PREFIX + email, code, 5, TimeUnit.MINUTES);
    }

    /**
     * 사용자가 입력한 인증 번호가 Redis에 저장된 번호와 일치하는지 검증합니다.
     * 검증에 성공하면 10분간 비밀번호 재설정이 가능한 상태로 표시하고, 재사용 방지를 위해 코드는 즉시 삭제합니다.
     *
     * @param email     검증할 사용자의 이메일 주소
     * @param inputCode 사용자가 입력한 6자리 인증 번호
     * @return 인증 성공 여부 (일치하면 true, 불일치하거나 만료되었으면 false)
     */
    public boolean verifyCode(String email, String inputCode) {
        String savedCode = stringRedisTemplate.opsForValue().get(PW_RESET_AUTH_PREFIX + email);

        if (savedCode == null || !savedCode.equals(inputCode)) {
            return false;
        }
        stringRedisTemplate.opsForValue().set(PW_RESET_VERIFIED_PREFIX + email, "DONE", 10, TimeUnit.MINUTES);
        stringRedisTemplate.delete(PW_RESET_AUTH_PREFIX + email);
        return true;
    }

    private String generateCode() {
        return String.valueOf(new Random().nextInt(899999) + 100000);
    }
}
