package sogeun.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sogeun.backend.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);
    @Modifying
    @Query(value = "TRUNCATE TABLE users", nativeQuery = true)
    void truncateUsers();

    List<User> findByUserIdIn(List<Long> userIds);
    List<User> findAllById(Iterable<Long> ids);


}

