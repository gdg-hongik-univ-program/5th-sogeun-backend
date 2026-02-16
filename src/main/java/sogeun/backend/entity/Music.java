package sogeun.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import sogeun.backend.sse.dto.MusicDto;
//import sogeun.backend.dto.request.MusicInfo;

@Getter
@Entity
@Table(
        name = "music",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_music_track",
                        columnNames = {"track_id"}
                )
        }
)
public class Music {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 아이튠즈 trackId (외부 고유 ID)
    @Column(name = "track_id", nullable = false, unique = true)
    private Long trackId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String artist;

    @Column(name = "artwork_url")
    private String artworkUrl;

    @Column(name = "preview_url")
    private String previewUrl;

    protected Music() {}

    public static Music of(MusicDto info) {
        Music music = new Music();
        music.trackId = info.getTrackId();
        music.title = info.getTitle();
        music.artist = info.getArtist();
        music.artworkUrl = info.getArtworkUrl();
        music.previewUrl = info.getPreviewUrl();
        return music;
    }

}
