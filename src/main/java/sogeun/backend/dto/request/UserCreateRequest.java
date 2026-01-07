package sogeun.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Schema(description = "회원가입 요청 DTO")
@Getter
public class UserCreateRequest {

    @NotBlank(message = "로그인 아이디는 필수입니다.")
    @Size(min = 2, max = 10, message = "로그인 아이디는 2~10자여야 합니다.")
    @Schema(description = "로그인 아이디 (중복 불가)", example = "sogeun123")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
    @Schema(description = "비밀번호", example = "password123!")
    private String password;

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 10, message = "닉네임은 2~10자여야 합니다.")
    @Schema(description = "닉네임", example = "소근")
    private String nickname;
}
