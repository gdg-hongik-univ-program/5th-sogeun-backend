package sogeun.backend.sse.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
//import sogeun.backend.dto.request.MusicInfo;

@Getter
public class BroadcastChangeMusicRequest {

    @NotNull
    private MusicDto music;
}
