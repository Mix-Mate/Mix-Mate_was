package com.mixmate.config;


import com.mixmate.exception.CustomAccessDeniedHandler;
import com.mixmate.exception.CustomAuthenticationEntryPoint;
import com.mixmate.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * TODO: auth 기능이 머지되기 전까지 인증을 임시로 비활성화한 상태입니다.
 *       auth 담당자가 로그인/토큰 발급 API를 붙이면 아래 주석 3곳
 *       (jwtAuthenticationFilter 필드 / authorizeHttpRequests / addFilterBefore)을
 *       되살리면 원래 설정으로 복구됩니다.
 *
 *       현재는 로그인 API가 없어 토큰을 발급받을 방법이 없기 때문에,
 *       인증을 켜두면 모든 API가 401이 되어 다른 도메인 개발이 불가능합니다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    // private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sess ->
                        sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // TODO: auth 머지 전까지 임시로 전체 허용. 아래 원본 설정으로 복구할 것.
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                // .authorizeHttpRequests(auth -> auth
                //         .requestMatchers("/api/v1/user/login").permitAll()
                //         .requestMatchers("/api/v1/user/signup/**").permitAll()
                //         .requestMatchers("/api/v1/auth/**").permitAll()
                //         .requestMatchers("/swagger-resources/**", "/swagger-ui/**", "/v3/api-docs/**",
                //                 "/webjars/**", "/error").permitAll()
                //         .anyRequest().authenticated()
                // )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                );
                // .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
