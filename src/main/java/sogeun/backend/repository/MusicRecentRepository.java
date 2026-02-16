package sogeun.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sogeun.backend.entity.MusicRecent;

import java.util.List;
import java.util.Optional;

public interface MusicRecentRepository extends JpaRepository<MusicRecent, Long> {

    // recordRecent()에서 사용
    Optional<MusicRecent> findByUser_UserIdAndMusic_Id(Long userId, Long musicId);

    // getRecentSongs()에서 사용
    List<MusicRecent> findByUser_UserIdOrderByLastPlayedAtDesc(Long userId);
}
