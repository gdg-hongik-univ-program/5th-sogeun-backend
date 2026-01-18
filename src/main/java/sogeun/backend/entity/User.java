package sogeun.backend.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor

public class  User {

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

    public User(String loginId, String password, String nickname) {
        this.loginId = loginId;
        this.password = password;
        this.nickname = nickname;
    }

    public void UpdateNickname(String nickname) {
        this.nickname = nickname;
    }

    @ManyToOne
    @JoinColumn(name = "favorite_song_id")
    private Song favoriteSong;

    public void updateFavoriteSong(Song song) {
        this.favoriteSong = song;
    }


}


