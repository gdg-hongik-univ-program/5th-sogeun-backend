//package sogeun.backend.entity;
//
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//
//import java.time.LocalDateTime;
//
//@Getter
//@NoArgsConstructor
//@Entity
//@Table(
//        name = "broadcast_like",
//        uniqueConstraints = {
//                @UniqueConstraint(
//                        name = "uk_broadcast_like",
//                        columnNames = {"broadcast_id", "liker_user_id"}
//                )
//        }
//)
//public class BroadcastLike {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "like_id")
//    private Long likeId;
//
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(
//            name = "broadcast_id",
//            nullable = false,
//            foreignKey = @ForeignKey(name = "fk_broadcast_like_broadcast")
//    )
//    private Broadcast broadcast;
//
//    @Column(name = "liker_user_id", nullable = false)
//    private Long likerUserId;
//
//    @Column(name = "like_count", nullable = false)
//    private long likeCount;
//
//    @Column(name = "created_at", nullable = false)
//    private LocalDateTime createdAt;
//
//    @Column(name = "updated_at", nullable = false)
//    private LocalDateTime updatedAt;
//
//    public void increment() {
//        this.likeCount++;
//        this.updatedAt = LocalDateTime.now();
//    }
//
//    public static BroadcastLike create(Broadcast broadcast, Long likerUserId) {
//        BroadcastLike like = new BroadcastLike();
//        like.broadcast = broadcast;
//        like.likerUserId = likerUserId;
//        like.likeCount = 1;
//        like.createdAt = LocalDateTime.now();
//        like.updatedAt = like.createdAt;
//        return like;
//    }
//}
