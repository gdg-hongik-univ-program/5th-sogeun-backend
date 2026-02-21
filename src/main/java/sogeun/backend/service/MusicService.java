package sogeun.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sogeun.backend.common.error.AppException;
import sogeun.backend.common.error.ErrorCode;
import sogeun.backend.dto.request.MusicLikeRequest;
import sogeun.backend.dto.request.MusicRecentRequest;
import sogeun.backend.dto.response.SogeunLibraryResponse;
import sogeun.backend.dto.response.UserLikeSongResponse;
import sogeun.backend.dto.response.UserRecentSongResponse;
import sogeun.backend.entity.*;
import sogeun.backend.repository.*;
import sogeun.backend.sse.dto.MusicDto;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MusicService {

    private final MusicRepository musicRepository;
    private final MusicLikeRepository musicLikeRepository;
    private final MusicRecentRepository musicRecentRepository;
    private final UserRepository userRepository;
    private final BroadcastMusicLikeRepository broadcastMusicLikeRepository;

    // 음악 좋아요(토글)
    @Transactional
    public void toggleLike(Long userId, MusicLikeRequest request) {
        MusicDto info = request.getMusic();
        Music music = findOrCreate(info);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        boolean exists = musicLikeRepository.existsByUser_UserIdAndMusic_Id(userId, music.getId());

        if (exists) {
            musicLikeRepository.deleteByUserIdAndMusicId(userId, music.getId());
            log.info("[MUSIC-LIKE] toggled OFF userId={} trackId={}", userId, music.getTrackId());
        } else {
            MusicLike like = MusicLike.ofLike(user, music);
            musicLikeRepository.save(like);
            log.info("[MUSIC-LIKE] toggled ON userId={} trackId={}", userId, music.getTrackId());
        }
    }

    // 좋아요 목록 조회
    @Transactional(readOnly = true)
    public List<UserLikeSongResponse> getLikedSongs(Long userId) {
        return musicLikeRepository.findLikedMusics(userId).stream()
                .map(UserLikeSongResponse::new)
                .toList();
    }

    // 최근 재생 기록 (upsert)
    @Transactional
    public void recordRecent(Long userId, MusicRecentRequest request) {
        MusicDto info = request.getMusic();
        Music music = findOrCreate(info);

        long playedAt = (request.getPlayedAt() != null)
                ? request.getPlayedAt()
                : Instant.now().toEpochMilli();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        musicRecentRepository.findByUser_UserIdAndMusic_Id(userId, music.getId())
                .ifPresentOrElse(existing -> {
                    existing.markPlayed(playedAt);
                }, () -> {
                    MusicRecent recent = MusicRecent.ofRec(user, music, playedAt);
                    musicRecentRepository.save(recent);
                });

        log.info("[MUSIC-RECENT] recorded userId={} trackId={} playedAt={}", userId, music.getTrackId(), playedAt);
    }

    // 최근 재생 목록 조회
    @Transactional(readOnly = true)
    public List<UserRecentSongResponse> getRecentSongs(Long userId) {
        return musicRecentRepository.findByUser_UserIdOrderByLastPlayedAtDesc(userId).stream()
                .map(UserRecentSongResponse::new)
                .toList();
    }

    // trackId로 음악 검색 후 없으면 생성
    @Transactional
    public Music findOrCreate(MusicDto info) {
        if (info == null || info.getTrackId() == null) {
            log.warn("[MUSIC] missing trackId");
            throw new AppException(ErrorCode.MUSIC_TRACK_ID_REQUIRED);
        }

        Long trackId = info.getTrackId();

        return musicRepository.findByTrackId(trackId)
                .orElseGet(() -> {
                    Music saved = musicRepository.save(Music.of(info));
                    log.info("[MUSIC] created trackId={}", trackId);
                    return saved;
                });
    }

    @Transactional(readOnly = true)
    public SogeunLibraryResponse getSogeunStats(Long userId) {
        List<BroadcastMusicLike> rows =
                broadcastMusicLikeRepository.findAllBySenderIdOrderByLikeCountDesc(userId);

        if (rows.isEmpty()) {
            log.info("[SOGEUN-STATS] userId={} empty", userId);
            return SogeunLibraryResponse.builder()
                    .totalTracks(0)
                    .totalLikes(0)
                    .tracks(List.of())
                    .build();
        }

        List<Long> trackIds = rows.stream()
                .map(BroadcastMusicLike::getTrackId)
                .distinct()
                .toList();

        List<Music> musics = musicRepository.findAllByTrackIdIn(trackIds);

        java.util.Map<Long, Music> musicMap = musics.stream()
                .collect(java.util.stream.Collectors.toMap(Music::getTrackId, m -> m));

        int totalLikes = rows.stream().mapToInt(BroadcastMusicLike::getLikeCount).sum();

        List<SogeunLibraryResponse.TrackStat> tracks = rows.stream()
                .map(r -> {
                    Music m = musicMap.get(r.getTrackId());
                    return SogeunLibraryResponse.TrackStat.builder()
                            .trackId(r.getTrackId())
                            .title(m != null ? m.getTitle() : null)
                            .artist(m != null ? m.getArtist() : null)
                            .artworkUrl(m != null ? m.getArtworkUrl() : null)
                            .likeCount(r.getLikeCount())
                            .build();
                })
                .toList();

        log.info("[SOGEUN-STATS] userId={} tracks={} totalLikes={}", userId, tracks.size(), totalLikes);

        return SogeunLibraryResponse.builder()
                .totalTracks(tracks.size())
                .totalLikes(totalLikes)
                .tracks(tracks)
                .build();
    }
}
