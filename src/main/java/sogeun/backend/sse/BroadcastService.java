package sogeun.backend.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//import sogeun.backend.dto.request.MusicInfo;
import sogeun.backend.common.exception.ConflictException;
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
    public void turnOn(Long senderId, double lat, double lon, MusicDto music) {
        log.info("[BROADCAST-ON] start senderId={}, lat={}, lon={}", senderId, lat, lon);

        if (music == null || music.getTrackId() == null) {
            throw new IllegalArgumentException("music.trackId is required");
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

        log.info("[BROADCAST-ON] done senderId={}, radius={}, targetCount={}",
                senderId, radius, targetUserIds.size());
    }



    @Transactional
    public void turnOff(Long senderId) {
        activeSenders.remove(senderId);

        Broadcast broadcast = broadcastRepository.findBySenderId(senderId)
                .orElseThrow(() -> new IllegalArgumentException("broadcast not found"));

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



    @Transactional
    public void toggleLike(Long senderId, Long likerId) {

        log.info("[LIKE] toggle start senderId={}, likerId={}", senderId, likerId);

        if (senderId.equals(likerId)) {
            log.warn("[LIKE] ignored self-like senderId={}", senderId);
            return;
        }

        Broadcast broadcast = broadcastRepository.findBySenderId(senderId)
                .orElseThrow(() -> new IllegalArgumentException("broadcast not found"));

        int oldLikeCount = broadcast.getLikeCount();
        int oldRadius = broadcast.getRadiusMeter();

        log.info(
                "[LIKE] before toggle senderId={} likeCount={} radius={}",
                senderId, oldLikeCount, oldRadius
        );

        boolean liked;

        liked = broadcastLikeRepository
                .findByBroadcast_BroadcastIdAndLikerUserId(broadcast.getBroadcastId(), likerId)
                .map(existing -> {
                    broadcastLikeRepository.delete(existing);
                    broadcast.decreaseLikeCount();
                    log.info("[LIKE] unlike senderId={} by userId={}", senderId, likerId);
                    return false;
                })
                .orElseGet(() -> {
                    broadcastLikeRepository.save(BroadcastLike.create(broadcast, likerId));
                    broadcast.increaseLikeCount();
                    log.info("[LIKE] like senderId={} by userId={}", senderId, likerId);
                    return true;
                });

        int newLikeCount = broadcast.getLikeCount();
        int newRadius = broadcast.getRadiusMeter();

        log.info(
                "[LIKE] after toggle senderId={} liked={} likeCount {}→{} radius {}→{}",
                senderId,
                liked,
                oldLikeCount, newLikeCount,
                oldRadius, newRadius
        );

        Point p = locationService.getLocation(senderId);
        if (p == null) {
            log.warn("[LIKE] no location senderId={}", senderId);
            return;
        }

        log.info(
                "[LIKE] location senderId={} lat={} lon={}",
                senderId, p.getY(), p.getX()
        );

        List<Long> targetUserIds =
                locationService.findNearbyUsersWithRadius(
                        senderId, p.getY(), p.getX(), newRadius
                );

        log.info(
                "[LIKE] target calculated senderId={} targetCount={} targets={}",
                senderId, targetUserIds.size(), targetUserIds
        );

        BroadcastEventDto event = BroadcastEventDto.likeUpdated(
                senderId,
                newLikeCount,
                newRadius
        );

        sendToTargets(targetUserIds, "broadcast.like", senderId, event);

        log.info("[LIKE] toggle done senderId={}", senderId);
    }






    private void sendToTargets(
            List<Long> targetUserIds,
            String eventName,
            Long senderId,
            Object data
    ) {
        log.info(
                "[SSE-SEND] start event={} senderId={} targetCount={} registrySize={}",
                eventName,
                senderId,
                targetUserIds.size(),
                registry.size()
        );

        int success = 0;
        int skippedSelf = 0;
        int noEmitter = 0;
        int failed = 0;

        for (Long targetId : targetUserIds) {

            if (targetId.equals(senderId)) {
                skippedSelf++;
                log.debug("[SSE-SEND] skip self targetId={}", targetId);
                continue;
            }

            SseEmitter emitter = registry.get(targetId);
            if (emitter == null) {
                noEmitter++;
                log.debug("[SSE-SEND] no emitter targetId={}", targetId);
                continue;
            }

            try {
                emitter.send(
                        SseEmitter.event()
                                .name(eventName)
                                .data(data, MediaType.APPLICATION_JSON)
                );

                success++;
                log.info(
                        "[SSE-SEND] success event={} senderId={} targetId={}",
                        eventName, senderId, targetId
                );

            } catch (IOException e) {
                failed++;
                registry.remove(targetId);
                log.warn(
                        "[SSE-SEND] failed event={} senderId={} targetId={} reason={}",
                        eventName, senderId, targetId, e.toString()
                );
            }
        }

        log.info(
                "[SSE-SEND] done event={} senderId={} success={} skippedSelf={} noEmitter={} failed={}",
                eventName, senderId, success, skippedSelf, noEmitter, failed
        );
    }


    //송출 중 음악 변경
    @Transactional
    public void changeMusic(Long userId, MusicDto musicDto) {
        Broadcast broadcast = broadcastRepository.findBySenderIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new IllegalStateException("방송 중이 아닙니다."));

        // 같은 음악이면 막기 (또는 return)
        Long currentTrackId = (broadcast.getMusic() != null) ? broadcast.getMusic().getTrackId() : null;
        Long newTrackId = (musicDto != null) ? musicDto.getTrackId() : null;


        if (currentTrackId != null && currentTrackId.equals(newTrackId)) {
            // 정책 B: 409로 보내고 싶으면 예외를 커스텀해서 핸들러에서 409 매핑
            throw new IllegalStateException("이미 같은 음악입니다.");
            // 정책 A면: return;
        }

        Music music = musicService.findOrCreate(musicDto);
        broadcast.updateCurrentMusic(music);
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





    //근처의 '송출중인 유저'만 검색
//    @Transactional(readOnly = true)
//    public List<UserNearbyResponse> findNearbyUsersWithBroadcast(List<Long> ids) {
//        if (ids.isEmpty()) return Collections.emptyList();
//
//        List<User> users = userRepository.findAllById(ids);
//        List<Broadcast> broadcasts = broadcastRepository.findBySenderIdInAndIsActiveTrue(ids);
//
//        Map<Long, Broadcast> broadcastMap = broadcasts.stream()
//                .collect(Collectors.toMap(Broadcast::getSenderId, b -> b));
//
//        return users.stream()
//                .map(user -> {
//                    Broadcast b = broadcastMap.get(user.getUserId());
//                    MusicResponse musicDto = null;
//                    if (b != null && b.getMusic() != null) {
//                        Music m = b.getMusic();
//                        musicDto = new MusicResponse(
//                                m.getTrackId(), m.getTitle(), m.getArtist(), m.getArtworkUrl(), m.getPreviewUrl()
//                        );
//                    }
//
//                    return new UserNearbyResponse(
//                            user.getUserId(), user.getNickname(), b != null,
//                            musicDto, b != null ? b.getRadiusMeter() : null, b != null ? b.getLikeCount() : 0
//                    );
//                })
//                .toList();
//    }
}