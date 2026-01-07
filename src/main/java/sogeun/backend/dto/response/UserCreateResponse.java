package sogeun.backend.dto.response;

import org.springframework.http.ResponseEntity;
import sogeun.backend.entity.User;

public class UserCreateResponse {

    private Long UserId;
    private String loginId;
    private String password;
    private String nickname;

    public static ResponseEntity<Void> of(User savedUser) {
        return null;
    }
}
