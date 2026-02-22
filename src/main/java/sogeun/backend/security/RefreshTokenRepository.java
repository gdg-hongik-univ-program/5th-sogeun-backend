package sogeun.backend.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshTokenRepository {

    private final StringRedisTemplate redisTemplate;

    public RefreshTokenRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String key(Long userId) {
        return "refresh:" + userId;
    }

    //refreshToken 저장 + TTL 설정
    public void save(Long userId, String refreshToken, Duration ttl) {
        redisTemplate.opsForValue().set(key(userId), refreshToken, ttl);
    }

    //저장된 refreshToken 조회
    public String get(Long userId) {
        return redisTemplate.opsForValue().get(key(userId));
    }

    //refreshToken 삭제
    public void delete(Long userId) {
        redisTemplate.delete(key(userId));
    }
}
