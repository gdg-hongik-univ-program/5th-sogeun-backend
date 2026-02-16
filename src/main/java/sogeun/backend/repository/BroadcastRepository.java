package sogeun.backend.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import sogeun.backend.entity.Broadcast;

import java.util.List;
import java.util.Optional;

public interface BroadcastRepository extends JpaRepository<Broadcast, Long> {

    Optional<Broadcast> findBySenderId(Long senderId);
    List<Broadcast> findBySenderIdInAndIsActiveTrue(List<Long> senderIds);

    Optional<Broadcast> findBySenderIdAndIsActiveTrue(Long userId);
}
