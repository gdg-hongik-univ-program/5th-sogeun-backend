package sogeun.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import sogeun.backend.entity.User;

@Getter
@AllArgsConstructor
public class UserCreateResponse {

    private Long userId;
    private String loginId;
    private String nickname;

    public static UserCreateResponse from(User user) {
        return new UserCreateResponse(
                user.getUserId(),
                user.getLoginId(),
                user.getNickname()
        );
    }
}
