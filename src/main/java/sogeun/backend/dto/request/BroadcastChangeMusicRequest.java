package sogeun.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import sogeun.backend.sse.dto.MusicDto;
//import sogeun.backend.dto.request.MusicInfo;

@Getter
public class BroadcastChangeMusicRequest {

    @NotNull
    private MusicDto music;
}
