package sogeun.backend.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import sogeun.backend.dto.response.UserNearbyResponse;
import sogeun.backend.service.LocationService;
import sogeun.backend.service.UserService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sse/location")
public class LocationController {

    private final LocationService locationService;
    private final UserService userService;


    @PostMapping("/update")
    public void updateLocation(
            @RequestParam Long userId,
            @RequestParam double lat,
            @RequestParam double lon
    ) {
        locationService.updateAndNotify(userId, lat, lon);
    }

    @GetMapping("/subscribe")
    public SseEmitter subscribe(@RequestParam Long userId) {
        return locationService.subscribe(userId);
    }

    @GetMapping("/nearby")
    public List<UserNearbyResponse> nearby(
            @RequestParam Long userId,
            @RequestParam double lat,
            @RequestParam double lon
    ) {
        List<Long> ids = locationService.findNearbyUsers(userId, lat, lon, 500);
        return userService.findUsersWithSong(ids);
    }

}
