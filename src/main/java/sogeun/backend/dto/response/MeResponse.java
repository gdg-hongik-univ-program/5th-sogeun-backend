package sogeun.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MeResponse {
    private Long userId; // 내부 회원 번호 (PK)
    private String loginId; // 로그인 아이디
    private String nickname; //닉네임
}
