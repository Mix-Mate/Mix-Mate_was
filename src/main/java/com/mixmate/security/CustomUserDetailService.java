package com.mixmate.security;

import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {
    private final UserRepository userRepository;

    /**
     * 입력받은 이메일을 통해 사용자를 조회하고 인증 객체를 생성합니다.
     * 계정의 인증 상태 체크하는 비즈니스 로직이 포함되어 있습니다.
     *
     * @param email 로그인 시도 이메일
     * @return 인증된 사용자의 정보를 담은 CustomUserDetails 객체
     * @throws UsernameNotFoundException 이메일에 해당하는 유저가 없을 경우 발생
     * @throws CustomException 유저가 이메일 인증(CERTIFIED) 상태가 아닐 경우 발생
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 계정 정지 기능이 포함되어 불가피하게 DB조회 로직 추가
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new UsernameNotFoundException("이메일로 사용자를 찾을 수 없음"));
        if(user.getVeriStatus() != VerificationStatus.CERTIFIED)
            throw new CustomException(ErrorCode.USER_NOT_CERTIFIED);

        return new CustomUserDetails(user);
    }

}