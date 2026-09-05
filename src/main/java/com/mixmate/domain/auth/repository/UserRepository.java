package com.mixmate.domain.auth.repository;

import com.mixmate.domain.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 이메일 기반 탐색
     *
     * @param email 이메일
     * @return User || null에 대한 예외처리 (orElse 사용필수)
     */
    Optional<User> findByEmail(String email);

    /**
     * 탈퇴하지 않은(활성) 사용자만 조회. 로그인·JWT 인증·비밀번호 재설정·토큰 재발급처럼
     * "이 계정을 실제로 쓸 수 있는가"를 판단해야 하는 곳에서 findByEmail 대신 사용한다.
     *
     * @param email 이메일
     * @return 탈퇴하지 않은 User || null에 대한 예외처리 (orElse 사용필수)
     */
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    /**
     * 해당 이메일을 가진 사용자가 DB에 이미 존재하는지
     *
     * @param email 이메일
     * @return
     */
    boolean existsByEmail(String email);

    // TODO: 초기 ERD에 nickname 컬럼이 없어(user_name이 그 역할인지 불명) 우선 제거합니다.
    //       Spring Data가 기동 시점에 프로퍼티를 찾지 못해 앱이 뜨지 않던 원인입니다.
    //       auth 담당자가 User 설계를 확정하면 복구할 것.
    // boolean existsByNickname(String nickname);
}
