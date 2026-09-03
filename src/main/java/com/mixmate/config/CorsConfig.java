package com.mixmate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * SecurityConfig가 주입받는 CorsConfigurationSource 빈입니다.
 * 이 빈이 없어서 애플리케이션 컨텍스트 로딩이 실패하고 있었습니다.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "https://mix-mate-web.vercel.app",
                "https://mix-mate-web-*.vercel.app", // PR/브랜치별 Vercel 프리뷰 배포 주소도 허용
                "http://165.229.244.149:3000" // 팀원 모바일 테스트용 임시 허용, 테스트 끝나면 제거
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
