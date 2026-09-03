package com.mixmate.security;


import com.mixmate.domain.auth.repository.UserRepository;
import com.mixmate.exception.CustomException;
import com.mixmate.exception.ErrorCode;
import com.mixmate.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RedisService redisService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // Redis 저장 시 키 앞에 붙일 접두사 (구분용)
    private static final String RT_PREFIX = "REFTOKEN:";

    /**
     * 리프레시 토큰을 Redis에 저장 (TTL은 7일)
     */
    public void saveRefreshToken(Long userId, String refreshToken) {
        // JwtUtil의 refreshValid와 동일하게 7일
        redisService.setDataExpire(RT_PREFIX + userId, refreshToken, jwtUtil.getRefreshValid());
    }

    /**
     * 회원 탈퇴/로그아웃 시 Redis에서 리프레시 토큰 삭제
     */
    public void deleteRefreshToken(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        redisService.deleteData(RT_PREFIX + userId);
    }

    /**
     * accessToken 재발급 시, 클라이언트가 보낸 리프레시 토큰이 로그인 때 발급해 저장해둔 것과
     * 같은지 대조하기 위해 조회한다. 로그아웃했거나 다른 기기에서 재로그인해 값이 바뀌었으면 없거나 다르다.
     */
    public String getRefreshToken(Long userId) {
        return redisService.getData(RT_PREFIX + userId);
    }
}