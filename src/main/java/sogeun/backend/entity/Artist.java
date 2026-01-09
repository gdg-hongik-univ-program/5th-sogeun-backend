package sogeun.backend.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "artists")
@Getter
@NoArgsConstructor


public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "artist_id") // 기본키
    private Long artistId;

    @Column(nullable = false, length = 50)
    private String name; // 중복허용 -> 동명이인 (나중에 아이디로 구분하는거 필요)

    protected Artist(String name) {
        this.name = name;
    }

}
