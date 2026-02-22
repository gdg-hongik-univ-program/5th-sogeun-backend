package sogeun.backend.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateLocationRequest {
    private double lat;
    private double lon;
}
