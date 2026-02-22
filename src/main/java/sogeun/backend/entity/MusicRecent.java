package sogeun.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "music_recent",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_music_recent_user_music",
                        columnNames = {"user_id", "music_id"}
                )
        }
)
public class MusicRecent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recent_id")
    private Long recentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_music_recent_user")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "music_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_music_recent_music")
    )
    private Music music;

    @Column(name = "last_played_at", nullable = false)
    private Instant lastPlayedAt;

    @Column(name = "play_count", nullable = false)
    private long playCount;

    private MusicRecent(User user, Music music, Instant playedAt) {
        this.user = user;
        this.music = music;
        this.lastPlayedAt = playedAt;
        this.playCount = 1L;
    }

    //최초 생성
    public static MusicRecent ofRec(User user, Music music, long playedAtMillis) {
        return new MusicRecent(user, music, Instant.ofEpochMilli(playedAtMillis));
    }

    //같은곡 다시 재생
    public void markPlayed(long playedAtMillis) {
        this.lastPlayedAt = Instant.ofEpochMilli(playedAtMillis);
        this.playCount++;
    }
}
