package sogeun.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "broadcast_music_like",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sender_track",
                        columnNames = {"sender_id", "track_id"}
                )
        },
        indexes = {
                @Index(name = "idx_sender", columnList = "sender_id"),
                @Index(name = "idx_track", columnList = "track_id")
        }
)
public class BroadcastMusicLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "track_id", nullable = false)
    private Long trackId;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Version
    private Long version;

    private BroadcastMusicLike(Long senderId, Long trackId, int likeCount) {
        this.senderId = senderId;
        this.trackId = trackId;
        this.likeCount = likeCount;
    }

    public static BroadcastMusicLike of(Long senderId, Long trackId, int likeCount) {
        return new BroadcastMusicLike(senderId, trackId, likeCount);
    }

    public void updateLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }
}