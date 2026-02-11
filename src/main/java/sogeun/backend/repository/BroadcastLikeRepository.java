package sogeun.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sogeun.backend.entity.BroadcastLike;

import java.util.Optional;

public interface BroadcastLikeRepository extends JpaRepository<BroadcastLike, Long> {

    Optional<BroadcastLike> findByBroadcast_BroadcastIdAndLikerUserId(Long broadcastId, Long likerUserId);

//    long countByBroadcast_BroadcastId(Long broadcastId);
}
