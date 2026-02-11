package sogeun.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sogeun.backend.common.exception.UnauthorizedException;
import sogeun.backend.common.message.ErrorMessage;
import sogeun.backend.dto.request.MusicInfo;
import sogeun.backend.dto.request.MusicLikeRequest;
import sogeun.backend.dto.request.MusicRecentRequest;
import sogeun.backend.dto.response.UserLikeSongResponse;
import sogeun.backend.entity.Music;
import sogeun.backend.entity.MusicLike;
import sogeun.backend.entity.MusicRecent;
import sogeun.backend.entity.User;
import sogeun.backend.repository.MusicLikeRepository;
import sogeun.backend.repository.MusicRecentRepository;
import sogeun.backend.repository.MusicRepository;
import sogeun.backend.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MusicService {

    private final MusicRepository musicRepository;
    private final MusicLikeRepository musicLikeRepository;
    private final MusicRecentRepository musicRecentRepository;
    private final UserRepository userRepository;

    //음악 좋아요
    @Transactional
    public void like(Long userId, MusicLikeRequest request) {
        MusicInfo info = request.getMusic();
        Music music = findOrCreate(info);

        // 중복 좋아요 방지
        if (musicLikeRepository.existsByUser_UserIdAndMusic_Id(userId, music.getId())) {
            return; // 또는 409
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException(ErrorMessage.UNAUTHORIZED));

        MusicLike like = MusicLike.ofLike(user, music);
        musicLikeRepository.save(like);
    }

    //좋아요 취소
    @Transactional
    public void deleteLike(Long userId, Long trackId) {
        Optional<Music> musicOpt = musicRepository.findByTrackId(trackId);
        if (musicOpt.isEmpty()) return;

        Long musicId = musicOpt.get().getId();

        // ✅ like 테이블에서 삭제해야 함
        musicLikeRepository.deleteByUserIdAndMusicId(userId, musicId);
    }


    //최근 재생 기록
    @Transactional
    public void recordRecent(Long userId, MusicRecentRequest request) {
        MusicInfo info = request.getMusic();
        Music music = findOrCreate(info);

        long playedAt = (request.getPlayedAt() != null)
                ? request.getPlayedAt()
                : Instant.now().toEpochMilli();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException(ErrorMessage.UNAUTHORIZED));

        musicRecentRepository.findByUser_UserIdAndMusic_Id(userId, music.getId())
                .ifPresentOrElse(existing -> {
                    existing.markPlayed(playedAt);
                }, () -> {
                    MusicRecent recent = MusicRecent.ofRec(user, music, playedAt);
                    musicRecentRepository.save(recent);
                });
    }

    //trackId로 음악 검색 후 없으면 생성
    @Transactional
    public Music findOrCreate(MusicInfo info) {
        if (info == null || info.getTrackId() == null) {
            throw new IllegalArgumentException("trackId is required");
        }

        Long trackId = info.getTrackId();

        return musicRepository.findByTrackId(trackId)
                .orElseGet(() -> musicRepository.save(Music.of(info)));
    }

    @Transactional(readOnly = true)
    public List<UserLikeSongResponse> getLikedSongs(Long userId) {
        return musicLikeRepository.findLikedMusics(userId).stream()
                .map(UserLikeSongResponse::new)
                .toList();
    }
}
