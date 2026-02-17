package sogeun.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sogeun.backend.common.exception.UnauthorizedException;
import sogeun.backend.common.message.ErrorMessage;
import sogeun.backend.dto.request.MusicLikeRequest;
import sogeun.backend.dto.request.MusicRecentRequest;
import sogeun.backend.dto.response.UserLikeSongResponse;
import sogeun.backend.dto.response.UserRecentSongResponse;
import sogeun.backend.entity.Music;
import sogeun.backend.entity.MusicLike;
import sogeun.backend.entity.MusicRecent;
import sogeun.backend.entity.User;
import sogeun.backend.repository.MusicLikeRepository;
import sogeun.backend.repository.MusicRecentRepository;
import sogeun.backend.repository.MusicRepository;
import sogeun.backend.repository.UserRepository;
import sogeun.backend.sse.dto.MusicDto;

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

    //음악 좋아요(토글)
    @Transactional
    public void toggleLike(Long userId, MusicLikeRequest request) {
        MusicDto info = request.getMusic();
        Music music = findOrCreate(info);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException(ErrorMessage.UNAUTHORIZED));

        boolean exists = musicLikeRepository.existsByUser_UserIdAndMusic_Id(userId, music.getId());

        if (exists) {
            musicLikeRepository.deleteByUserIdAndMusicId(userId, music.getId());
        } else {
            MusicLike like = MusicLike.ofLike(user, music);
            musicLikeRepository.save(like);
        }
    }



    //좋아요 목록 조회
    @Transactional(readOnly = true)
    public List<UserLikeSongResponse> getLikedSongs(Long userId) {
        return musicLikeRepository.findLikedMusics(userId).stream()
                .map(UserLikeSongResponse::new)
                .toList();
    }

    //최근 재생 기록 (upsert)
    @Transactional
    public void recordRecent(Long userId, MusicRecentRequest request) {
        MusicDto info = request.getMusic();
        Music music = findOrCreate(info);

        long playedAt = (request.getPlayedAt() != null)
                ? request.getPlayedAt()
                : Instant.now().toEpochMilli();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException(ErrorMessage.UNAUTHORIZED));

        musicRecentRepository.findByUser_UserIdAndMusic_Id(userId, music.getId())
                .ifPresentOrElse(existing -> {
                    // 기존 기록 업데이트(시간 갱신 + playCount 증가 등은 markPlayed 구현에 맞춰)
                    existing.markPlayed(playedAt);
                }, () -> {
                    MusicRecent recent = MusicRecent.ofRec(user, music, playedAt);
                    musicRecentRepository.save(recent);
                });

        // (선택) 최근 목록 개수 제한 같은 정책이 있으면 여기서 정리
        // trimRecentIfNeeded(userId, 50);
    }

    //최근 재생 목록 조회
    @Transactional(readOnly = true)
    public List<UserRecentSongResponse> getRecentSongs(Long userId) {
        // ✅ 아래 둘 중 하나로 구현하면 됨.
        // 1) MusicRecentRepository에서 Recent 엔티티 리스트 가져오기
        return musicRecentRepository.findByUser_UserIdOrderByLastPlayedAtDesc(userId).stream()
                .map(UserRecentSongResponse::new)
                .toList();

        // 2) 만약 너가 "최근 재생 음악만(Music)"을 바로 뽑는 커스텀 쿼리를 만들었다면:
        // return musicRecentRepository.findRecentMusics(userId).stream()
        //        .map(UserRecentSongResponse::new) // 이 경우 생성자가 Music 기반이면 바꿔야 함
        //        .toList();
    }

    //trackId로 음악 검색 후 없으면 생성
    @Transactional
    public Music findOrCreate(MusicDto info) {
        if (info == null || info.getTrackId() == null) {
            throw new IllegalArgumentException("trackId is required");
        }

        Long trackId = info.getTrackId();

        return musicRepository.findByTrackId(trackId)
                .orElseGet(() -> musicRepository.save(Music.of(info)));
    }

    // (선택) 최근 50개 제한 같은 정책이 필요하면 Repository/Query 만들어서 여기서 정리
    // private void trimRecentIfNeeded(Long userId, int limit) {
    //     // 예: 마지막 playedAt 기준으로 limit 초과분 삭제 쿼리
    // }
}
