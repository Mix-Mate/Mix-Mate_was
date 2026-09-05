package com.mixmate.domain.auth.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("탈퇴하면 탈퇴 시각이 찍히고, 이메일이 반납되도록 값 자체가 바뀐다")
    void withdrawMarksDeletedAtAndFreesEmail() {
        User user = User.builder()
                .userId(42L)
                .userName("김대현")
                .email("kdh@example.com")
                .password("encoded-password")
                .build();

        user.withdraw();

        assertThat(user.getDeletedAt()).isNotNull();
        // 원래 이메일이 unique 컬럼 값으로 그대로 남아있으면 재가입이 막히므로, 값 자체가 바뀌어야 한다.
        assertThat(user.getEmail()).isNotEqualTo("kdh@example.com");
        assertThat(user.getEmail()).contains("42").contains("kdh@example.com");
    }
}
