package sogeun.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtProvider {

    private final Key key;

    // Access Token 만료 시간 (분 단위)
    private final int accessExpMinutes;

    // Refresh Token 만료 시간 (일 단위)  ← 추가
    private final int refreshExpDays;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-exp-minutes}") int accessExpMinutes,
            @Value("${jwt.refresh-token-exp-days}") int refreshExpDays // 추가
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpMinutes = accessExpMinutes;
        this.refreshExpDays = refreshExpDays;
    }

    /**
     * Access Token 생성
     */
    public String createAccessToken(Long userId) {
        Instant now = Instant.now();
        Instant exp = now.plus(accessExpMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("typ", "access")               // ✅ 토큰 타입 추가
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Refresh Token 생성  ← ✅ 추가
     */
    public String createRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant exp = now.plus(refreshExpDays, ChronoUnit.DAYS);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("typ", "refresh")              // ✅ 토큰 타입 추가
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * JWT 토큰에서 userId 추출
     */
    public Long parseUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return Long.valueOf(claims.getSubject());
    }

    /**
     * JWT 토큰 타입(typ) 추출  ← ✅ 추가
     */
    public String parseTokenType(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Object typ = claims.get("typ");
        return typ == null ? null : typ.toString();
    }

    /**
     * JWT 유효성 검증
     */
    public boolean validate(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;

        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        return "access".equals(parseTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(parseTokenType(token));
    }
}
