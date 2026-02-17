package sogeun.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sogeun.backend.dto.request.MusicLikeRequest;
import sogeun.backend.dto.request.MusicRecentRequest;
import sogeun.backend.dto.response.UserLikeSongResponse;
import sogeun.backend.dto.response.UserRecentSongResponse;
import sogeun.backend.service.MusicService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MusicController {

    private final MusicService musicService;

    //음악 좋아요 추가(토글)
    // 음악 좋아요 토글
    @PostMapping("/update/music/likes")
    public ResponseEntity<Void> toggleLikeMusic(
            Authentication authentication,
            @RequestBody MusicLikeRequest request
    ) {
        Long userId = Long.valueOf(authentication.getName());
        musicService.toggleLike(userId, request);
        return ResponseEntity.ok().build(); // 200
    }


    //좋아요한 음악 리스트
    @GetMapping("/library/likes")
    public ResponseEntity<List<UserLikeSongResponse>> getMyLikedSongs(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        List<UserLikeSongResponse> result = musicService.getLikedSongs(userId);
        return ResponseEntity.ok(result);
    }


    /**
     * 최근 재생 기록 저장 (upsert)
     * POST /api/update/music/recent
     */
    @PostMapping("/update/music/recent")
    public ResponseEntity<Void> recordRecent(Authentication authentication,
                                             @RequestBody MusicRecentRequest request) {
        Long userId = Long.valueOf(authentication.getName());
        musicService.recordRecent(userId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 최근 재생 목록 조회
     * GET /api/library/recent
     */
    @GetMapping("/library/recent")
    public ResponseEntity<List<UserRecentSongResponse>> getMyRecent(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        return ResponseEntity.ok(musicService.getRecentSongs(userId));
    }
}
