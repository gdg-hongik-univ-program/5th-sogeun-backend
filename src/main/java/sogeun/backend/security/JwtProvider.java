package sogeun.backend.security;

// JWT 생성, 파싱, 검증을 위한 jjwt 라이브러리
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component // Spring Bean으로 등록 → 인증/필터/서비스 등에서 주입 가능
public class JwtProvider {

    // JWT 서명(Signature)에 사용할 비밀키
    // HS256(HMAC-SHA256) 알고리즘용 Key 객체
    private final Key key;

    // Access Token 만료 시간 (분 단위)
    private final int expMinutes;

    /**
     * application.yml(properties)에 정의된 값을 주입받아
     * JWT 서명용 Key와 만료 시간을 초기화한다.
     */
    public JwtProvider(
            @Value("${jwt.secret}") String secret, // JWT 비밀키
            @Value("${jwt.access-token-exp-minutes}") int expMinutes // 만료 시간(분)
    ) {
        // 문자열 secret을 byte 배열로 변환 후
        // HMAC-SHA256에 사용할 Key 객체 생성
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        // Access Token 만료 시간 설정
        this.expMinutes = expMinutes;
    }

    /**
     * Access Token 생성
     * @param userId 인증된 사용자의 고유 ID
     * @return JWT Access Token 문자열
     */
    public String createAccessToken(Long userId) {

        // 현재 시각
        Instant now = Instant.now();

        // 현재 시각 + 만료 시간(expMinutes)
        Instant exp = now.plus(expMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                // JWT의 subject(sub) 값
                // → userId를 식별자로 사용
                .setSubject(String.valueOf(userId))

                // 토큰 발급 시간 (iat)
                .setIssuedAt(Date.from(now))

                // 토큰 만료 시간 (exp)
                .setExpiration(Date.from(exp))

                // 서명 알고리즘 및 비밀키 설정
                .signWith(key, SignatureAlgorithm.HS256)

                // JWT 문자열 생성
                .compact();
    }

    /**
     * JWT 토큰에서 userId 추출
     * @param token Authorization 헤더에서 추출한 순수 JWT
     * @return 토큰에 담긴 userId
     */
    public Long parseUserId(String token) {

        // 토큰 서명 검증 + 파싱 수행
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key) // 서명 검증에 사용할 키
                .build()
                .parseClaimsJws(token) // 서명·만료 검증 수행
                .getBody(); // Payload(Claims) 추출

        // subject(sub)에 저장된 userId 반환
        return Long.valueOf(claims.getSubject());
    }

    /**
     * JWT 유효성 검증
     * - 서명 위조 여부
     * - 만료(exp) 여부
     * - 토큰 형식 오류 여부
     */
    public boolean validate(String token) {
        try {
            // 파싱 시도 자체가 검증 로직
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            // 예외가 발생하지 않으면 유효한 토큰
            return true;

        } catch (JwtException | IllegalArgumentException e) {
            // 서명 오류, 만료, 변조, 형식 오류 등
            return false;
        }
    }
}
