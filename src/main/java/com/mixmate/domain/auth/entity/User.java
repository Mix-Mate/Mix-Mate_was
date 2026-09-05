package com.mixmate.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    /** NULL이면 활성 계정. 탈퇴 시점이 찍히면 로그인·JWT 인증이 모두 거부된다. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /**
     * email에 unique 제약이 걸려있어, 탈퇴 후에도 원래 이메일을 그대로 두면 같은 이메일로 재가입할 수 없다.
     * userId를 붙여 값 자체를 바꿔치기해 원래 이메일을 반납하면서도, 이 행 자체의 유니크 제약은 그대로 만족시킨다.
     */
    public void withdraw() {
        this.deletedAt = LocalDateTime.now();
        this.email = "withdrawn:" + this.userId + ":" + this.email;
    }
}
