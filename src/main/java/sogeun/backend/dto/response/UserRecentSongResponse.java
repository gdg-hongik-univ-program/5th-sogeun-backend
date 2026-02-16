package sogeun.backend.dto.response;

import lombok.Getter;
import sogeun.backend.entity.Music;
import sogeun.backend.entity.MusicRecent;

import java.time.Instant;

@Getter
public class UserRecentSongResponse {

    private Long musicId;        // 내부 PK
    private Long trackId;        // 프론트 트랙 ID
    private String title;
    private String artist;
    private String artworkUrl;
    private String previewUrl;

    private Instant lastPlayedAt;   // 마지막 재생 시각 (epoch millis)
    private Long playCount;      // 재생 횟수

    public UserRecentSongResponse(MusicRecent recent) {
        Music music = recent.getMusic();

        this.musicId = music.getId();
        this.trackId = music.getTrackId();
        this.title = music.getTitle();
        this.artist = music.getArtist();
        this.artworkUrl = music.getArtworkUrl();
        this.previewUrl = music.getPreviewUrl();

        this.lastPlayedAt = recent.getLastPlayedAt();
        this.playCount = recent.getPlayCount();
    }
}
