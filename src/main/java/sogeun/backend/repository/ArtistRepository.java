package sogeun.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sogeun.backend.entity.Artist;

import java.util.Optional;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    Optional<Artist> findByName(String name);

}
