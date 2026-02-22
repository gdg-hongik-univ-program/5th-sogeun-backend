package sogeun.backend.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Service;
import sogeun.backend.common.error.AppException;
import sogeun.backend.common.error.ErrorCode;
import sogeun.backend.entity.Broadcast;
import sogeun.backend.repository.BroadcastRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private static final String KEY = "geo:user";

    private final RedisTemplate<String, String> redisTemplate;
    private final BroadcastRepository broadcastRepository;

    // 현재 송출중인 유저만 위치 저장
    public void saveLocation(Long userId, double lat, double lon) {

        boolean isBroadcasting = broadcastRepository
                .findBySenderIdAndIsActiveTrue(userId)
                .isPresent();

        if (!isBroadcasting) return;

        redisTemplate.opsForGeo().add(KEY, new Point(lon, lat), userId.toString());
    }

    // 저장된 위치 조회
    public Point getLocation(Long userId) {
        List<Point> positions = redisTemplate.opsForGeo().position(KEY, userId.toString());
        return (positions == null || positions.isEmpty()) ? null : positions.get(0);
    }

    // 반경 내 유저 검색
    public List<Long> findNearbyUsersWithRadius(Long myId, double lat, double lon, double radiusMeter) {

        Circle circle = new Circle(new Point(lon, lat), new Distance(radiusMeter, Metrics.METERS));

        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
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

}