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
     * 해당 이메일을 가진 사용자가 DB에 이미 존재하는지
     *
     * @param email 이메일
     * @return
     */
    boolean existsByEmail(String email);

    /**
     * 해당 닉네임을 가진 사용자가 DB에 이미 존재하는지
     *
     * @param nickname 닉네임
     * @return
     */
    boolean existsByNickname(String nickname);
}
