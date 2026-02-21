package sogeun.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sogeun.backend.entity.BroadcastMusicLike;

import java.util.List;
import java.util.Optional;

public interface BroadcastMusicLikeRepository extends JpaRepository<BroadcastMusicLike, Long> {
    Optional<BroadcastMusicLike> findBySenderIdAndTrackId(Long senderId, Long trackId);

    // 소근 통계용
    List<BroadcastMusicLike> findAllBySenderIdOrderByLikeCountDesc(Long senderId);
}