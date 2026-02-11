package sogeun.backend.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sogeun.backend.sse.dto.UserNearbyResponse;
import sogeun.backend.service.UserService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sse/location")
public class LocationController {

    private final LocationService locationService;
    private final UserService userService;
    private final BroadcastService broadcastService;


    @PostMapping("/update")
    public void updateLocation(
            @RequestParam Long userId,
            @RequestParam double lat,
            @RequestParam double lon
    ) {
        locationService.updateAndNotify(userId, lat, lon);
    }

    //이건 기존 연결로 대체
//    @GetMapping("/subscribe")
//    public SseEmitter subscribe(@RequestParam Long userId) {
//        return locationService.subscribe(userId);
//    }
//

//    이거는 song때문에 잠시 주석..
    @GetMapping("/nearby")
    public List<UserNearbyResponse> nearby(
            @RequestParam Long userId,
            @RequestParam double lat,
            @RequestParam double lon //이거 dto로 변환해야함
    ) {
        List<Long> ids = locationService.findNearbyUsers(userId, lat, lon, 500);
        return broadcastService.findNearbyUsersWithBroadcast(ids);
    }

}