package com.mixmate.security;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    //    @Value("${jwt.secret-key}")
    private final SecretKey secretKey;

    @Getter
    private final long accessValid = 1000L * 60 * 60; // 1시간
    @Getter
    private final long refreshValid = 1000L * 60 * 60 * 24 * 7; // 7일

    public JwtUtil(@Value("${jwt.secret-key}") String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // 1. Access 토큰 발급
    public String createAccessToken(String email) {
        return generateToken(email, accessValid);
    }
    // 2. Refresh 토큰 발급
    public String createRefreshToken(String email) {
        return generateToken(email, refreshValid);
    }

    // 토큰 발급 종합
    private String generateToken(String email, long validTime) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + validTime))
//                .signWith(Keys.hmacShaKeyFor(secretKey.getEncoded()), SignatureAlgorithm.HS256)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Authorization 헤더의 Bearer 토큰을 우선 확인하고, 없으면 accessToken 쿠키를 확인합니다.
     * 로그인이 쿠키로 토큰을 내려주기 때문에, 브라우저 요청은 대부분 쿠키 경로로 인증됩니다.
     */
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 제거
        }
        return extractCookie(request, "accessToken");
    }

    public String extractRefreshTokenFromCookie(HttpServletRequest request) {
        return extractCookie(request, "refreshToken");
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public long getExpiration(String token) {
        // 토큰의 전체 페이로드에서 만료 시간(Expiration)을 가져옴
        Date expiration = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();

        // 현재 시간과의 차이를 계산 (밀리초 단위)
        long now = new Date().getTime();
        long diff = expiration.getTime() - now;
        return diff > 0 ? diff : 0L; // 0보다 작으면 그냥 0 반환
    }


}
