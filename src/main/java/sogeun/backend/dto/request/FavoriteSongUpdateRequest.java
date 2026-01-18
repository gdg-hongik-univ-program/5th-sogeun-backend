package sogeun.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public class FavoriteSongUpdateRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String artistName;

    public String getTitle() {
        return title;
    }

    public String getArtistName() {
        return artistName;
    }
}
