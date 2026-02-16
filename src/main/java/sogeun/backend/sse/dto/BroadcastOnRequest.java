package sogeun.backend.sse.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.ToString;
//import sogeun.backend.dto.request.MusicInfo;

@ToString
@Getter
public class BroadcastOnRequest {

    @NotNull(message = "위도")
    private Double lat;

    @NotNull(message = "경도")
    private Double lon;

    @NotNull(message = "음악")
    @Valid
    private MusicDto music;
}
