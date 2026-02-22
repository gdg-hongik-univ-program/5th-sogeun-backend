package sogeun.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_loginid", columnNames = "user_loginid"),
                @UniqueConstraint(name = "uk_users_nickname", columnNames = "user_nickname")
        }
)
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_loginid", nullable = false, unique = true, length = 50)
    private String loginId;

    @Column(name = "user_pw", nullable = false, length = 100)
    private String password;

    @Column(name = "user_nickname", nullable = false, unique = true, length = 20)
    private String nickname;

    // 좋아요한 음악들
    @OneToMany(
            mappedBy = "user",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<MusicLike> likedMusics = new ArrayList<>();

    // 최근 재생 음악들
    @OneToMany(
            mappedBy = "user",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<MusicRecent> recentMusics = new ArrayList<>();

    public User(String loginId, String password, String nickname) {
        this.loginId = loginId;
        this.password = password;
        this.nickname = nickname;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}