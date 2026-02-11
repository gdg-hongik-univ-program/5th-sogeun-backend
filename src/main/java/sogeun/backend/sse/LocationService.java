package sogeun.backend.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import sogeun.backend.repository.BroadcastRepository;
import sogeun.backend.sse.dto.NearbyUserEvent;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private static final String KEY = "geo:user";
    private final RedisTemplate<String, String> redisTemplate;
    private final SseEmitterRegistry registry;
    private final BroadcastRepository broadcastRepository;

    public void saveLocation(Long userId, double lat, double lon) {
        redisTemplate.opsForGeo().add(KEY, new Point(lon, lat), userId.toString());
    }

    public List<Long> findNearbyUsers(Long myId, double lat, double lon, double radiusMeter) {
        Circle circle = new Circle(new Point(lon, lat), new Distance(radiusMeter, Metrics.METERS));
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .sortAscending().includeDistance();

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().radius(KEY, circle, args);
        if (results == null) return List.of();

        return results.getContent().stream()
                .map(r -> Long.valueOf(r.getContent().getName()))
                .filter(id -> !id.equals(myId))
                .toList();
    }

    // 위치 업데이트 시 방송 중이면 그 방송의 반경을, 아니면 기본 200m 사용
    public void updateAndNotify(Long userId, double lat, double lon) {
        saveLocation(userId, lat, lon);

        double radius = broadcastRepository.findBySenderId(userId)
                .map(b -> (double) b.getRadiusMeter())
                .orElse(200.0);

        log.info("[NEARBY-CHECK] userId={}, applied radius={}m", userId, radius);

        List<Long> nearby = findNearbyUsers(userId, lat, lon, radius);

        for (Long targetId : nearby) {
            SseEmitter emitter = registry.get(targetId);
            if (emitter == null) continue;
            try {
                emitter.send(SseEmitter.event().name("NEARBY_USER").data(new NearbyUserEvent(userId)));
            } catch (IOException e) {
                registry.remove(targetId);
            }
        }
    }

    public Point getLocation(Long userId) {
        List<Point> positions = redisTemplate.opsForGeo().position(KEY, userId.toString());
        return (positions == null || positions.isEmpty()) ? null : positions.get(0);
    }
}