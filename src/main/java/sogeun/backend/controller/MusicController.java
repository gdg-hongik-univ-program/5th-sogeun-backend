package sogeun.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sogeun.backend.dto.request.MusicLikeRequest;
import sogeun.backend.dto.request.MusicRecentRequest;
import sogeun.backend.dto.response.UserLikeSongResponse;
import sogeun.backend.service.MusicService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MusicController {

    private final MusicService musicService;

    /**
     * 좋아요 추가
     * - 요청 DTO 안에 MusicInfo(music)가 들어있고, 서비스에서 find-or-create 후 like 처리
     */
    @PostMapping("/update/music/likes")
    public ResponseEntity<Void> likeMusic(Authentication authentication,
                                          @RequestBody MusicLikeRequest request) {
        Long userId = Long.valueOf(authentication.getName()); // 필터가 name에 userId 넣는 구조일 때
        musicService.like(userId, request);
        return ResponseEntity.ok().build();
    }

    // GET /api/music/likes
    @GetMapping("/library/likes")
    public ResponseEntity<List<UserLikeSongResponse>> getMyLikedSongs(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName()); // ← 너희 JWT에서 sub=userId면 이게 제일 깔끔
        List<UserLikeSongResponse> result = musicService.getLikedSongs(userId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/update/music/likes/delete/{trackId}")
    public ResponseEntity<Void> deleteLike(
            @PathVariable Long trackId,
            Authentication authentication
    ) {
        Long userId = Long.valueOf(authentication.getName());
        musicService.deleteLike(userId, trackId);
        return ResponseEntity.noContent().build(); // 204
    }

//    /**
//     * 최근 재생 기록
//     * - 요청 DTO 안에 MusicInfo(music) + playedAt(옵션)
//     * - 서비스에서 find-or-create 후 recent upsert/갱신 처리
//     */
//    @PostMapping("/update/recent")
//    public ResponseEntity<Void> recordRecent(Authentication authentication,
//                                             @RequestBody MusicRecentRequest request) {
//        Long userId = Long.valueOf(authentication.getName());
//        musicService.record(userId, request);
//        return ResponseEntity.ok().build();
//    }
}
