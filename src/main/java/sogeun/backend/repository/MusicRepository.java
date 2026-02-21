package sogeun.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sogeun.backend.entity.Music;

import java.util.List;
import java.util.Optional;

@Repository
public interface MusicRepository extends JpaRepository<Music, Long> {

    Optional<Music> findByTrackId(Long trackId);
    List<Music> findAllByTrackIdIn(List<Long> trackIds);
}
