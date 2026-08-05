package com.mixmate.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 엔티티입니다. 초기 ERD의 User 테이블을 그대로 옮긴 최소 형태입니다.
 *
 * TODO: auth 담당자가 인증 설계를 시작하면 이 엔티티를 인계할 것.
 *       현재는 security 패키지 컴파일을 위해 필요한 최소 필드만 있습니다.
 */
@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name", nullable = false, length = 50)
    private String userName;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /**
     * TODO: 초기 ERD에 password 컬럼이 없습니다.
     *       ERD에 phone_number만 있는 걸 보면 소셜 로그인이나 전화번호 인증일 가능성이 있는데,
     *       Spring Security의 UserDetails 규격상 getPassword()가 필요해 우선 필드만 잡아둡니다.
     *       로그인 방식이 확정되면 auth 담당자가 정리할 것.
     */
    @Column(name = "password")
    private String password;
}
