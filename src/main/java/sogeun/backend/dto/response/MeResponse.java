package sogeun.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
//import sogeun.backend.entity.Song;
import sogeun.backend.entity.User;

@Getter
//@AllArgsConstructor
public class MeResponse {

    private Long userId;
    private String loginId;
    private String nickname;
//    private String favoriteSongTitle;
//    private String favoriteArtistName;

    public MeResponse(
            Long userId,
            String loginId,
            String nickname
//            String favoriteSongTitle,
//            String favoriteArtistName
    ) {
        this.userId = userId;
        this.loginId = loginId;
        this.nickname = nickname;
//        this.favoriteSongTitle = favoriteSongTitle;
//        this.favoriteArtistName = favoriteArtistName;
    }


}

