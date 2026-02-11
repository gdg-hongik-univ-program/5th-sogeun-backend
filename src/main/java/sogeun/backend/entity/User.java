package sogeun.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_loginid")
    private String loginId;

    @Column(name = "user_pw", length = 100)
    private String password;

    @Column(name = "user_nickname", length = 20)
    private String nickname;

    //좋아요한 음악들
    @OneToMany(
            mappedBy = "user",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<MusicLike> likedMusics = new ArrayList<>();

    //최근 재생 음악들
    @OneToMany(
            mappedBy = "user",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<MusicRecent> recentMusics = new ArrayList<>();


//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "favorite_song_id")
//    private Song favoriteSong;

    public User(String loginId, String password, String nickname) {
        this.loginId = loginId;
        this.password = password;
        this.nickname = nickname;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

//    public void updateFavoriteSong(Song song) {
//        this.favoriteSong = song;
//    }
}
