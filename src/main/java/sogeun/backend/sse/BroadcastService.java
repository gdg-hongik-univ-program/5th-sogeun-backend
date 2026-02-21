package sogeun.backend.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import sogeun.backend.common.error.AppException;
import sogeun.backend.common.error.ErrorCode;
import sogeun.backend.entity.Broadcast;
import sogeun.backend.entity.BroadcastMusicLike;
import sogeun.backend.entity.Music;
import sogeun.backend.repository.BroadcastMusicLikeRepository;
import sogeun.backend.repository.BroadcastRepository;
import sogeun.backend.repository.UserRepository;
import sogeun.backend.service.MusicService;
import sogeun.backend.sse.dto.*;


import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastService {

    private final SseEmitterRegistry registry;
    private final LocationService locationService;
    private final BroadcastRepository broadcastRepository;
    private final UserRepository userRepository;
    private final MusicService musicService;
    private final BroadcastMusicLikeRepository broadcastMusicLikeRepository;

    private final Set<Long> activeSenders = ConcurrentHashMap.newKeySet();

    @Transactional
    public void turnOn(Long senderId, double lat, double lon, MusicDto music) {
        log.info("[BROADCAST-ON] senderId={} lat={} lon={}", senderId, lat, lon);

        if (music == null || music.getTrackId() == null) {
            throw new AppException(ErrorCode.MUSIC_TRACK_ID_REQUIRED);
        }

        Broadcast broadcast = broadcastRepository.findBySenderId(senderId)
                .orElseGet(() -> broadcastRepository.save(Broadcast.create(senderId)));

        Music musicEntity = musicService.findOrCreate(music);

        broadcast.updateCurrentMusic(musicEntity);
        broadcast.activate();

        broadcast.updateRadiusByLikes();
        int radius = broadcast.getRadiusMeter();

        locationService.saveLocation(senderId, lat, lon);

        List<Long> targetUserIds =
                locationService.findNearbyUsersWithRadius(senderId, lat, lon, radius);

        BroadcastEventDto event = BroadcastEventDto.on(senderId, music);

        activeSenders.add(senderId);
        sendToTargets(targetUserIds, "broadcast.on", senderId, event);

        log.info("[BROADCAST-ON] done senderId={} radius={} targets={}", senderId, radius, targetUserIds.size());
    }

    @Transactional
    public void turnOff(Long senderId) {
        log.info("[BROADCAST-OFF] senderId={}", senderId);

        activeSenders.remove(senderId);

        Broadcast broadcast = broadcastRepository.findBySenderId(senderId)
                .orElseThrow(() -> new AppException(ErrorCode.BROADCAST_NOT_FOUND));

        int radius = broadcast.getRadiusMeter();

        Point p = locationService.getLocation(senderId);

        broadcast.deactivate();

        if (p == null) return;

        List<Long> targetUserIds =
                locationService.findNearbyUsersWithRadius(
                        senderId, p.getY(), p.getX(), radius
                );

        BroadcastEventDto event = BroadcastEventDto.off(senderId);
        sendToTargets(targetUserIds, "broadcast.off", senderId, event);
    }

    private MusicDto toMusicDto(Music m) {
        if (m == null) return null;
        return new MusicDto(
                m.getTrackId(),
                m.getTitle(),
                m.getArtist(),
                m.getArtworkUrl(),
                m.getPreviewUrl()
        );
    }

    @Transactional
    public void like(Long broadcastId, Long likerUserId) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new AppException(ErrorCode.BROADCAST_NOT_FOUND));

        // 송출중인지 체크
        if (!broadcast.isActive()) {
            broadcast.increaseLikeCount();
            return;
        }

        Long senderId = broadcast.getSenderId();

        //  반경 변경 전 값 저장
        int oldRadius = broadcast.getRadiusMeter();

        // 2like 증가 (+ 내부에서 radius 업데이트)
        broadcast.increaseLikeCount();

        // 현재 곡의 likeCount도 +1 저장
        Music cur = broadcast.getMusic();
        if (cur != null && cur.getTrackId() != null) {
            increaseSongLikeCount(senderId, cur.getTrackId());
        }

        // 반경 변경 후 값
        int newRadius = broadcast.getRadiusMeter();

        log.info("[BROADCAST-LIKE] broadcastId={} senderId={} likeCount={} radius {}->{}",
                broadcastId, senderId, broadcast.getLikeCount(), oldRadius, newRadius);

        // 반경이 실제로 안 변했으면 재전파x
        if (oldRadius == newRadius) {
            Point p = locationService.getLocation(senderId);
            if (p == null) return;

            double lat = p.getY();
            double lon = p.getX();

            List<Long> targets = locationService.findNearbyUsersWithRadius(senderId, lat, lon, newRadius);
            BroadcastLikeEventDto likeEvent =
                    BroadcastLikeEventDto.of(senderId, broadcastId, broadcast.getLikeCount(), newRadius);

            sendToTargets(targets, "broadcast.like", senderId, likeEvent);
            return;
        }

        Point p = locationService.getLocation(senderId);
        if (p == null) return;

        double lat = p.getY(); // y=lat
        double lon = p.getX(); // x=lon

        // oldTargets / newTargets 계산
        List<Long> oldTargets = locationService.findNearbyUsersWithRadius(senderId, lat, lon, oldRadius);
        List<Long> newTargets = locationService.findNearbyUsersWithRadius(senderId, lat, lon, newRadius);

        Set<Long> oldSet = new HashSet<>(oldTargets);
        Set<Long> newSet = new HashSet<>(newTargets);

        // joined = new - old
        Set<Long> joined = new HashSet<>(newSet);
        joined.removeAll(oldSet);

        // left = old - new
        Set<Long> left = new HashSet<>(oldSet);
        left.removeAll(newSet);

        // kept = intersection
        Set<Long> kept = new HashSet<>(newSet);
        kept.retainAll(oldSet);

        // joined에게는 broadcast.on
        MusicDto musicDto = toMusicDto(broadcast.getMusic());
        if (!joined.isEmpty() && musicDto != null) {
            BroadcastEventDto onEvent = BroadcastEventDto.on(senderId, musicDto);
            sendToTargets(joined.stream().toList(), "broadcast.on", senderId, onEvent);
        }

        // left에게는 broadcast.off
        if (!left.isEmpty()) {
            BroadcastEventDto offEvent = BroadcastEventDto.off(senderId);
            sendToTargets(left.stream().toList(), "broadcast.off", senderId, offEvent);
        }

        // kept에게는 broadcast.like
        if (!kept.isEmpty()) {
            BroadcastLikeEventDto likeEvent =
                    BroadcastLikeEventDto.of(senderId, broadcastId, broadcast.getLikeCount(), newRadius);
            sendToTargets(kept.stream().toList(), "broadcast.like", senderId, likeEvent);
        }
    }

    private void sendToTargets(
            List<Long> targetUserIds,
            String eventName,
            Long senderId,
            Object data
    ) {
        for (Long targetId : targetUserIds) {

            if (targetId.equals(senderId)) continue;

            SseEmitter emitter = registry.get(targetId);
            if (emitter == null) continue;

            try {
                emitter.send(
                        SseEmitter.event()
                                .name(eventName)
                                .data(data, MediaType.APPLICATION_JSON)
                );
            } catch (IOException e) {
                // 최소 로그: 실패만 warn
                log.warn("[SSE-SEND] failed event={} senderId={} targetId={} reason={}",
                        eventName, senderId, targetId, e.toString());
                registry.remove(targetId);
            }
        }
    }

    // 송출 중 음악 변경
    @Transactional
    public void changeMusic(Long userId, MusicDto musicDto) {
        Broadcast broadcast = broadcastRepository.findBySenderIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new AppException(ErrorCode.BROADCAST_NOT_ACTIVE));

        Long currentTrackId = (broadcast.getMusic() != null) ? broadcast.getMusic().getTrackId() : null;
        Long newTrackId = (musicDto != null) ? musicDto.getTrackId() : null;

        // 같은 음악이면 무시
        if (currentTrackId != null && currentTrackId.equals(newTrackId)) {
            return;
        }

        Music music = musicService.findOrCreate(musicDto);
        broadcast.updateCurrentMusic(music);

        log.info("[BROADCAST-MUSIC] userId={} trackId={}", userId, newTrackId);
    }

    @Transactional(readOnly = true)
    public MyBroadcastResponse getMyBroadcast(Long userId) {

        Broadcast broadcast = broadcastRepository.findBySenderId(userId)
                .orElse(null);

        // 방송 자체가 없는 경우
        if (broadcast == null) {
            return MyBroadcastResponse.builder()
                    .active(false)
                    .lat(null)
                    .lon(null)
                    .music(null)
                    .likeCount(0)
                    .build();
        }

        boolean active = broadcast.isActive();

        // 위치 정보 (Redis)
        Double lat = null;
        Double lon = null;
        Point p = locationService.getLocation(userId);
        if (p != null) {
            lon = p.getX();
            lat = p.getY();
        }

        MyBroadcastResponse.MusicDto musicDto = null;
        if (broadcast.getMusic() != null) {
            Music m = broadcast.getMusic();
            musicDto = MyBroadcastResponse.MusicDto.builder()
                    .trackId(m.getTrackId())
                    .title(m.getTitle())
                    .artist(m.getArtist())
                    .artworkUrl(m.getArtworkUrl())
                    .build();
        }

        // 방송 꺼져 있으면 음악/좌표 숨김 (정책)
        if (!active) {
            lat = null;
            lon = null;
            musicDto = null;
        }

        return MyBroadcastResponse.builder()
                .active(active)
                .lat(lat)
                .lon(lon)
                .music(musicDto)
                .likeCount(broadcast.getLikeCount())
                .build();
    }

    private int increaseSongLikeCount(Long senderId, Long trackId) {
        BroadcastMusicLike entity = broadcastMusicLikeRepository
                .findBySenderIdAndTrackId(senderId, trackId)
                .orElseGet(() -> BroadcastMusicLike.of(senderId, trackId, 0));

        int newCount = entity.getLikeCount() + 1;
        entity.updateLikeCount(newCount);
        broadcastMusicLikeRepository.save(entity);

        return newCount;
    }
}