package sogeun.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class BroadcastChangeMusicRequest {

    @NotNull
    private MusicInfo music;
}
