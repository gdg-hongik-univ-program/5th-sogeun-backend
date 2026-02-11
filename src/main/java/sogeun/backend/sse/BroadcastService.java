package sogeun.backend.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import sogeun.backend.dto.request.MusicInfo;
import sogeun.backend.entity.Broadcast;
import sogeun.backend.entity.BroadcastLike;
import sogeun.backend.entity.Music;
import sogeun.backend.entity.User;
import sogeun.backend.repository.BroadcastLikeRepository;
import sogeun.backend.repository.BroadcastRepository;
import sogeun.backend.repository.UserRepository;
import sogeun.backend.service.MusicService;
import sogeun.backend.sse.dto.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BroadcastService {

    private final SseEmitterRegistry registry;
    private final LocationService locationService;
    private final BroadcastRepository broadcastRepository;
    private final BroadcastLikeRepository broadcastLikeRepository;
    private final UserRepository userRepository;
    private final MusicService musicService;

    // 켜져 있는 사용자 상태 저장 (메모리)
    private final Set<Long> activeSenders = ConcurrentHashMap.newKeySet();

    public BroadcastService(
            SseEmitterRegistry registry,
            LocationService locationService,
            BroadcastRepository broadcastRepository,
            BroadcastLikeRepository broadcastLikeRepository,
            UserRepository userRepository,
            MusicService musicService
    ) {
        this.registry = registry;
        this.locationService = locationService;
        this.broadcastRepository = broadcastRepository;
        this.broadcastLikeRepository = broadcastLikeRepository;
        this.userRepository = userRepository;
        this.musicService = musicService;
    }

    @Transactional
    public void turnOn(Long senderId, double lat, double lon, MusicInfo musicInfo) {
        log.info("[BROADCAST-ON] start senderId={}, lat={}, lon={}", senderId, lat, lon);

        if (musicInfo == null || musicInfo.getTrackId() == null) {
            throw new IllegalArgumentException("musicInfo.trackId is required");
        }

        // 1) 위치 최신화 (Redis)
        locationService.saveLocation(senderId, lat, lon);

        // 2) broadcast 없으면 생성, 있으면 가져오기
        Broadcast broadcast = broadcastRepository.findBySenderId(senderId)
                .orElseGet(() -> broadcastRepository.save(Broadcast.create(senderId)));

        // 3) Music 엔티티 find-or-create
        Music music = musicService.findOrCreate(musicInfo);

        // 4) 상태 업데이트 (음악 설정 및 활성화)
        broadcast.updateCurrentMusic(music);
        broadcast.activate(); // isActive = true

        // 5) 엔티티에서 반경 계산 및 업데이트
        broadcast.updateRadiusByLikes();

        // 6) 갱신된 반경으로 주변 유저 조회
        List<Long> targetUserIds = locationService.findNearbyUsers(senderId, lat, lon, broadcast.getRadiusMeter());

        // 7) SSE 이벤트 전송
        MusicDto dto = new MusicDto(
                musicInfo.getTrackId(), musicInfo.getTrackName(), musicInfo.getArtistName(),
                musicInfo.getArtworkUrl(), musicInfo.getPreviewUrl()
        );
        BroadcastEventDto event = BroadcastEventDto.on(senderId, dto);

        activeSenders.add(senderId);
        sendToTargets(targetUserIds, "broadcast.on", senderId, event);

        log.info("[BROADCAST-ON] done senderId={}, radius={}, targetCount={}",
                senderId, broadcast.getRadiusMeter(), targetUserIds.size());
    }

    @Transactional
    public void turnOff(Long senderId) {
        activeSenders.remove(senderId);

        Broadcast broadcast = broadcastRepository.findBySenderId(senderId)
                .orElseThrow(() -> new IllegalArgumentException("broadcast not found"));

        broadcast.deactivate();

        Point p = locationService.getLocation(senderId);
        if (p == null) return;

        // 종료 알림 전송 (기존 저장된 반경 기준)
        List<Long> targetUserIds = locationService.findNearbyUsers(senderId, p.getY(), p.getX(), broadcast.getRadiusMeter());
        BroadcastEventDto event = BroadcastEventDto.off(senderId);
        sendToTargets(targetUserIds, "broadcast.off", senderId, event);
    }

    @Transactional
    public void toggleLike(BroadcastLikeRequest request) {
        Long senderId = request.getSenderId();
        Long likerId = request.getLikerId();

        if (senderId.equals(likerId)) return;

        Broadcast broadcast = broadcastRepository.findBySenderId(senderId)
                .orElseThrow(() -> new IllegalArgumentException("broadcast not found"));

        // 1) 좋아요 토글 및 반경 자동 갱신
        broadcastLikeRepository.findByBroadcast_BroadcastIdAndLikerUserId(broadcast.getBroadcastId(), likerId)
                .ifPresentOrElse(existing -> {
                    broadcastLikeRepository.delete(existing);
                    broadcast.decreaseLikeCount(); // 엔티티 내부에서 radiusMeter도 같이 계산되도록 설계 권장
                }, () -> {
                    broadcastLikeRepository.save(BroadcastLike.create(broadcast, likerId));
                    broadcast.increaseLikeCount();
                });

        // 2) 변경된 반경으로 타겟 재계산 (현재 위치 기준)
        Point p = locationService.getLocation(senderId);
        double lat = (p != null) ? p.getY() : request.getLat();
        double lon = (p != null) ? p.getX() : request.getLon();

        List<Long> targetUserIds = locationService.findNearbyUsers(senderId, lat, lon, broadcast.getRadiusMeter());

        // 3) SSE 전송
        BroadcastEventDto event = BroadcastEventDto.likeUpdated(
                senderId, broadcast.getLikeCount(), broadcast.getRadiusMeter()
        );
        sendToTargets(targetUserIds, "broadcast.like", senderId, event);
    }

    private void sendToTargets(List<Long> targetUserIds, String eventName, Long senderId, Object data) {
        for (Long targetId : targetUserIds) {
            if (targetId.equals(senderId)) continue;

            SseEmitter emitter = registry.get(targetId);
            if (emitter == null) continue;

            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                registry.remove(targetId);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<UserNearbyResponse> findNearbyUsersWithBroadcast(List<Long> ids) {
        if (ids.isEmpty()) return Collections.emptyList();

        List<User> users = userRepository.findAllById(ids);
        List<Broadcast> broadcasts = broadcastRepository.findBySenderIdInAndIsActiveTrue(ids);

        Map<Long, Broadcast> broadcastMap = broadcasts.stream()
                .collect(Collectors.toMap(Broadcast::getSenderId, b -> b));

        return users.stream()
                .map(user -> {
                    Broadcast b = broadcastMap.get(user.getUserId());
                    MusicResponse musicDto = null;
                    if (b != null && b.getMusic() != null) {
                        Music m = b.getMusic();
                        musicDto = new MusicResponse(
                                m.getTrackId(), m.getTitle(), m.getArtist(), m.getArtworkUrl(), m.getPreviewUrl()
                        );
                    }

                    return new UserNearbyResponse(
                            user.getUserId(), user.getNickname(), b != null,
                            musicDto, b != null ? b.getRadiusMeter() : null, b != null ? b.getLikeCount() : 0
                    );
                })
                .toList();
    }
}