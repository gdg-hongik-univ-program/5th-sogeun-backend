//package sogeun.backend.entity;
//
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//
//@Entity
//@Table(name = "songs")
//@Getter
//@NoArgsConstructor
//
//public class Song {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "song_id")
//    private Long songId; // 기본키 =/= 트랙아이디 -> 이건 내부 디비용
//
//    private Long trackId; // dto로 받을 트랙아이디
//
//    @Column(nullable = false, length = 100)
//    private String title; // 노래제목
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "artist_id", nullable = false)
//    private Artist artist; // 아티스트
//
//    private String artworkUrl; //앨범아트 주소
//
//    private String previewUrl; //미리듣기 주소
//
//    public Song(String title, Artist artist) {
//        this.title = title;
//        this.artist = artist;
//    }
//}
