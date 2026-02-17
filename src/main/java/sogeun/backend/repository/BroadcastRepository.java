package sogeun.backend.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sogeun.backend.entity.Broadcast;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BroadcastRepository extends JpaRepository<Broadcast, Long> {

    Optional<Broadcast> findBySenderId(Long senderId);
    List<Broadcast> findBySenderIdInAndIsActiveTrue(List<Long> senderIds);

    Optional<Broadcast> findBySenderIdAndIsActiveTrue(Long userId);

    @Modifying
    @Query("update Broadcast b set b.likeCount = b.likeCount + 1, b.updatedAt = :now where b.broadcastId = :id")
    int incrementLikeCount(@Param("id") Long broadcastId, @Param("now") LocalDateTime now);

}
