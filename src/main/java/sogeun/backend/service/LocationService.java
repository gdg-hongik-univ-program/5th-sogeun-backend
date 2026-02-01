package sogeun.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import sogeun.backend.sse.SseEmitterRegistry;
import sogeun.backend.sse.dto.NearbyUserEvent;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private static final String KEY = "geo:user";

    private final RedisTemplate<String, String> redisTemplate;
    private final SseEmitterRegistry registry;

    // =============================
    // 위치 저장
    // =============================
    public void saveLocation(Long userId, double lat, double lon) {
        redisTemplate.opsForGeo()
                .add(KEY, new Point(lon, lat), userId.toString());
    }

    // =============================
    // 반경 검색
    // =============================
    public List<Long> findNearbyUsers(
            Long myId,
            double lat,
            double lon,
            double radiusMeter) {

        Circle circle = new Circle(
                new Point(lon, lat),
                new Distance(radiusMeter, Metrics.METERS)
        );

        RedisGeoCommands.GeoRadiusCommandArgs args =
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                        .sortAscending()
                        .includeDistance();

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisTemplate.opsForGeo().radius(KEY, circle, args);

        if (results == null) return List.of();

        return results.getContent().stream()
                .map(r -> Long.valueOf(r.getContent().getName()))
                .filter(id -> !id.equals(myId))
                .toList();
    }

    // =============================
    // 위치 업데이트 + SSE 알림
    // =============================
    public void updateAndNotify(Long userId, double lat, double lon) {

        saveLocation(userId, lat, lon);

        List<Long> nearby = findNearbyUsers(userId, lat, lon, 500);

        for (Long target : nearby) {
            SseEmitter emitter = registry.get(target);
            if (emitter == null) continue;

            try {
                emitter.send(SseEmitter.event()
                        .name("NEARBY_USER")
                        .data(new NearbyUserEvent(userId)));
            } catch (IOException e) {
                registry.remove(target);
            }
        }
    }

    // =============================
    // SSE 구독 (에러 원인 해결)
    // =============================
    public SseEmitter subscribe(Long userId) {

        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L); // 5분

        registry.addOrReplace(userId, emitter);

        emitter.onCompletion(() -> registry.remove(userId));
        emitter.onTimeout(() -> registry.remove(userId));
        emitter.onError(e -> registry.remove(userId));

        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECT")
                    .data("connected"));
        } catch (IOException e) {
            registry.remove(userId);
        }

        return emitter;
    }
}
