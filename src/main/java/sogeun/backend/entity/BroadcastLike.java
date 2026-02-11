package sogeun.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "broadcast_like",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_broadcast_like",
                        columnNames = {"broadcast_id", "liker_user_id"}
                )
        }
)
public class BroadcastLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private Long likeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "broadcast_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_broadcast_like_broadcast")
    )
    private sogeun.backend.entity.Broadcast broadcast;

    @Column(name = "liker_user_id", nullable = false)
    private Long likerUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static BroadcastLike create(sogeun.backend.entity.Broadcast broadcast, Long likerUserId) {
        BroadcastLike like = new BroadcastLike();
        like.broadcast = broadcast;
        like.likerUserId = likerUserId;
        like.createdAt = LocalDateTime.now();
        return like;
    }
}
