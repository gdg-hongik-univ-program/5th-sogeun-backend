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

    public MeResponse(
            Long userId,
            String loginId,
            String nickname
    ) {
        this.userId = userId;
        this.loginId = loginId;
        this.nickname = nickname;
    }


}

