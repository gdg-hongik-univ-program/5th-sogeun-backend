//package sogeun.backend.repository;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Modifying;
//import org.springframework.data.jpa.repository.Query;
//import sogeun.backend.entity.BroadcastLike;
//
//import java.time.LocalDateTime;
//import java.util.Optional;
//
//public interface BroadcastLikeRepository extends JpaRepository<BroadcastLike, Long> {
//
//    @Modifying
//    @Query("""
//        update BroadcastLike bl
//           set bl.likeCount = bl.likeCount + 1,
//               bl.updatedAt = :now
//         where bl.broadcast.broadcastId = :broadcastId
//           and bl.likerUserId = :userId
//    """)
//    int increment(Long broadcastId, Long userId, LocalDateTime now);
//}
