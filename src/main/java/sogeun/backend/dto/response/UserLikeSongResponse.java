package sogeun.backend.dto.response;

import lombok.Getter;
import sogeun.backend.entity.Music;

@Getter
public class UserLikeSongResponse {

    private Long musicId;      // 내부 PK (선택)
    private Long trackId;    // 프론트 고유 트랙 ID
    private String title;
    private String artist;
    private String artworkUrl;
    private String previewUrl;

    public UserLikeSongResponse(Music music) {
        this.musicId = music.getId();        // Music PK 이름에 맞게 수정
        this.trackId = music.getTrackId();
        this.title = music.getTitle();
        this.artist = music.getArtist();
        this.artworkUrl = music.getArtworkUrl();
        this.previewUrl = music.getPreviewUrl();
    }

}
