package com.mixmate.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_provider_id", columnNames = {"provider", "provider_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name", nullable = false, length = 10)
    private String userName;

    @Column(nullable = false, unique = true)
    private String email;

    /** 소셜 로그인 계정은 비밀번호가 없다. */
    @Column
    private String password;

    /** 기존 로컬 회원가입 계정과 구분하기 위한 가입 경로. 컬럼 추가 시 기존 행은 전부 LOCAL로 채워진다. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'LOCAL'")
    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;

    /** OAuth 제공자가 내려주는 고유 식별자. LOCAL 계정은 null. */
    @Column(name = "provider_id")
    private String providerId;

    /** NULL이면 활성 계정. 탈퇴 시점이 찍히면 로그인·JWT 인증이 모두 거부된다. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static User ofKakao(String email, String userName, String providerId) {
        return User.builder()
                .email(email)
                .userName(userName)
                .provider(AuthProvider.KAKAO)
                .providerId(providerId)
                .build();
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateUserName(String userName) {
        this.userName = userName;
    }

    public boolean isLocal() {
        return this.provider == AuthProvider.LOCAL;
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
