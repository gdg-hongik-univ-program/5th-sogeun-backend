package sogeun.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sogeun.backend.entity.MusicRecent;

import java.util.Optional;

public interface MusicRecentRepository extends JpaRepository<MusicRecent, Long> {

    Optional<MusicRecent> findByUser_UserIdAndMusic_Id(Long userId, Long musicId);
}
