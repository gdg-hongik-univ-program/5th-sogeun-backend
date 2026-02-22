package sogeun.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sogeun.backend.entity.Music;
import sogeun.backend.entity.MusicLike;

import java.util.List;
import java.util.Optional;

public interface MusicLikeRepository extends JpaRepository<MusicLike, Long> {

    boolean existsByUser_UserIdAndMusic_Id(Long userId, Long musicId);

    @Modifying
    @Query("delete from MusicLike l where l.user.userId = :userId and l.music.id = :musicId")
    int deleteByUserIdAndMusicId(@Param("userId") Long userId, @Param("musicId") Long musicId);

    @Query("select l.music from MusicLike l where l.user.userId = :userId order by l.createdAt desc")
    List<Music> findLikedMusics(@Param("userId") Long userId);
}
