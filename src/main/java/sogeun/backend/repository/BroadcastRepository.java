package sogeun.backend.repository;


import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Broadcast b where b.id = :id")
    Optional<Broadcast> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Broadcast b where b.senderId = :senderId")
    Optional<Broadcast> findBySenderIdForUpdate(@Param("senderId") Long senderId);

}
