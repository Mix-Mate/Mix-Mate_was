package com.mixmate.security;


import com.mixmate.domain.auth.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security에서 사용하는 인증 사용자 정보 객체입니다.
 * DB 연관관계 이슈를 방지하기 위해 User 엔티티 전체가 아닌 필수 정보만 필드로 가집니다.
 */
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String email;
    private final String password;
    private final Long univId; // 인증정보에 대학 세션으로 구분되어야함

    /**
     * User 엔티티를 받아 CustomUserDetails 객체를 생성합니다.
     *
     * @param user DB에서 조회한 로그인 사용자 엔티티
     */
    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.univId = user.getUniv().getUnivId();
    }


    public Long getUserId() {
        return userId;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        // 시큐리티의 규격상 ID 역할을 하는 email을 반환
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 현재 권한 기능은 사용하지 않으므로 빈 리스트를 반환
        return Collections.emptyList();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

