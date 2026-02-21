package sogeun.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "broadcast",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_broadcast_sender",
                        columnNames = {"sender_id"}
                )
        }
)
public class Broadcast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "broadcast_id")
    private Long broadcastId;

    @Column(name = "sender_id", nullable = false, unique = true)
    private Long senderId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "music_id")
    private Music music;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "radius_meter", nullable = false)
    private int radiusMeter;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    public void prePersist() {
        this.startedAt = LocalDateTime.now();
        this.updatedAt = this.startedAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static Broadcast create(Long senderId) {
        Broadcast b = new Broadcast();
        b.senderId = senderId;
        b.likeCount = 0;
        b.radiusMeter = 200; // 초기 반경
        b.isActive = false;
        return b;
    }

    //좋아요 수에 따라 반경 계산
    public int calculateRadius() {
        if (this.likeCount < 10) return 200;
        if (this.likeCount < 30) return 400;
        if (this.likeCount < 60) return 600;
        return 800;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
        this.music = null;
    }

    public void updateCurrentMusic(Music music) {
        this.music = music;
    }

    public void updateRadiusByLikes() {
        this.radiusMeter = calculateRadius();
    }

    public void increaseLikeCount() {
        this.likeCount++;
        updateRadiusByLikes();
    }

    public void decreaseLikeCount() {
        this.likeCount = Math.max(0, this.likeCount - 1);
        updateRadiusByLikes();
    }


}