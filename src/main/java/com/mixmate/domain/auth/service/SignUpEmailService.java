package com.mixmate.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SignUpEmailService {

    private static final String EMAIL_AUTH_PREFIX = "EMAIL_AUTH:";
    private static final String VERIFIED_EMAIL_PREFIX = "VERIFIED_EMAIL:";

    private final JavaMailSender javaMailSender;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 사용자의 이메일로 6자리 인증 번호를 발송하고, 해당 번호를 Redis에 저장
     * 보안을 위해 저장된 번호는 5분(TTL) 후에 자동으로 삭제
     *
     * @param email 인증 번호를 수신할 사용자의 이메일 주소
     * @throws org.springframework.mail.MailException 메일 발송 중 오류가 발생할 경우 발생
     */
    public void sendVerificationCode(String email) {
        String code = generateCode();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[Mixmate] 회원가입 인증 번호입니다.");
        message.setText("인증 번호: " + code + "\n5분 이내에 입력해주세요.");

        javaMailSender.send(message);

        // Redis에 저장 (Key: EMAIL_AUTH:이메일, Value: 코드, TTL: 5분)
        stringRedisTemplate.opsForValue().set(EMAIL_AUTH_PREFIX + email, code, 5, TimeUnit.MINUTES);
    }

    /**
     * 사용자가 입력한 인증 번호가 Redis에 저장된 번호와 일치하는지 검증
     * 검증에 성공하면 중복 사용 및 보안 방지를 위해 즉시 Redis에서 해당 데이터를 삭제
     *
     * @param email     검증할 사용자의 이메일 주소
     * @param inputCode 사용자가 입력한 6자리 인증 번호
     * @return 인증 성공 여부 (일치하면 true, 불일치하거나 만료되었으면 false)
     */
    public boolean verifyCode(String email, String inputCode) {
        String savedCode = stringRedisTemplate.opsForValue().get(EMAIL_AUTH_PREFIX + email);

        if (savedCode == null || !savedCode.equals(inputCode)) {
            return false;
        }
        stringRedisTemplate.opsForValue().set(VERIFIED_EMAIL_PREFIX + email, "DONE", 10, TimeUnit.MINUTES);
        // 인증 성공 시 데이터 삭제 (중복 사용 방지)
        stringRedisTemplate.delete(EMAIL_AUTH_PREFIX + email);
        return true;
    }

    private String generateCode() {
        return String.valueOf(new Random().nextInt(899999) + 100000); // 6자리 난수
    }
}
