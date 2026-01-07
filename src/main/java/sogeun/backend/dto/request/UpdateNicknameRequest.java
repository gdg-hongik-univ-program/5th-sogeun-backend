package sogeun.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Schema(description = "닉네임 변경 요청 DTO")
@Getter
public class UpdateNicknameRequest {

    @NotBlank(message = "닉네임을 입력해주세요")
    @Size(min = 2, max = 10, message = "닉네임은 최소 2자 이상, 10자 미만이어야 합니다.")
    @Schema(description = "닉네임", example = "소근", required = true)
    public String nickname;
}
