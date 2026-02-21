package sogeun.backend.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.geo.Point;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//import sogeun.backend.dto.request.MusicInfo;
import sogeun.backend.common.exception.ConflictException;
import sogeun.backend.entity.Broadcast;
//import sogeun.backend.entity.BroadcastLike;
import sogeun.backend.entity.BroadcastMusicLike;
import sogeun.backend.entity.Music;
import sogeun.backend.entity.User;
//import sogeun.backend.repository.BroadcastLikeRepository;
import sogeun.backend.repository.BroadcastMusicLikeRepository;
import sogeun.backend.repository.BroadcastRepository;
import sogeun.backend.repository.UserRepository;
import sogeun.backend.service.MusicService;
import sogeun.backend.sse.dto.*;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    // 켜져 있는 사용자 상태 저장 (메모리)
    private final Set<Long> activeSenders = ConcurrentHashMap.newKeySet();

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
                .orElseThrow(() -> new IllegalArgumentException("broadcast not found"));

        // ✅ 방송 중인 경우에만 반경 전파 의미가 있음 (정책에 맞게)
        if (!broadcast.isActive()) {
            // 여기 분기는 "off면 좋아요 못 누름"이면 사실상 호출될 일이 없지만,
            // 혹시 모를 예외 상황 대비로 기존 로직 유지
            broadcast.increaseLikeCount();
            return;
        }

        Long senderId = broadcast.getSenderId();

        // 1) 반경 변경 전 값 저장
        int oldRadius = broadcast.getRadiusMeter();

        // 2) like 증가 (+ 내부에서 radius 업데이트)
        broadcast.increaseLikeCount();

        // ⭐ 추가: "현재 곡"의 곡별 likeCount도 +1 저장
        // (broadcast.likeCount는 반경용 누적이므로 건드리지 않음)
        Music cur = broadcast.getMusic();
        if (cur != null && cur.getTrackId() != null) {
            increaseSongLikeCount(senderId, cur.getTrackId());
        }

        // 3) 반경 변경 후 값
        int newRadius = broadcast.getRadiusMeter();

        // ✅ 반경이 실제로 안 변했으면 굳이 재전파할 필요 없음 (성능/깜빡임 방지)
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

        // 4) sender 위치가 있어야 타겟 계산 가능
        Point p = locationService.getLocation(senderId);
        if (p == null) return;

        double lat = p.getY(); // y=lat
        double lon = p.getX(); // x=lon

        // 5) oldTargets / newTargets 계산
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

        // 6) joined에게는 "broadcast.on" (신규 노출)
        MusicDto musicDto = toMusicDto(broadcast.getMusic());
        if (!joined.isEmpty() && musicDto != null) {
            BroadcastEventDto onEvent = BroadcastEventDto.on(senderId, musicDto);
            sendToTargets(joined.stream().toList(), "broadcast.on", senderId, onEvent);
        }

        // 7) left에게는 "broadcast.off" (범위 밖)
        if (!left.isEmpty()) {
            BroadcastEventDto offEvent = BroadcastEventDto.off(senderId);
            sendToTargets(left.stream().toList(), "broadcast.off", senderId, offEvent);
        }

        // 8) kept에게는 "broadcast.like" (업데이트: likeCount/radius)
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

    private int loadSongLikeCount(Long senderId, Long trackId) {
        return broadcastMusicLikeRepository
                .findBySenderIdAndTrackId(senderId, trackId)
                .map(BroadcastMusicLike::getLikeCount)
                .orElse(0);
    }

    private void saveSongLikeCount(Long senderId, Long trackId, int likeCount) {
        BroadcastMusicLike entity = broadcastMusicLikeRepository
                .findBySenderIdAndTrackId(senderId, trackId)
                .orElseGet(() -> BroadcastMusicLike.of(senderId, trackId, likeCount));

        entity.updateLikeCount(likeCount);
        broadcastMusicLikeRepository.save(entity);
    }

    private int increaseSongLikeCount(Long senderId, Long trackId) {
        BroadcastMusicLike entity = broadcastMusicLikeRepository
                .findBySenderIdAndTrackId(senderId, trackId)
                .orElseGet(() -> BroadcastMusicLike.of(senderId, trackId, 0));

        int newCount = entity.getLikeCount() + 1;
        entity.updateLikeCount(newCount);
        broadcastMusicLikeRepository.save(entity);

        return newCount; // 필요하면 이벤트/응답에 써도 됨
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