package sogeun.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "music_like",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_music_like_user_music",
                        columnNames = {"user_id", "music_id"}
                )
        }
)
public class MusicLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private Long likeId; // 사건(좋아요) 자체의 PK

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_music_like_user"))
    private User user; // FK

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "music_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_music_like_music"))
    private Music music; // FK

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public MusicLike(User user, Music music) {
        this.user = user;
        this.music = music;
    }

    public static MusicLike ofLike(User user, Music music) {
        return new MusicLike(user, music);
    }

}
