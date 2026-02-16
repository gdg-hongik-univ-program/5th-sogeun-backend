package sogeun.backend.sse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sogeun.backend.sse.dto.UpdateLocationRequest;
import sogeun.backend.service.UserService;
import sogeun.backend.sse.dto.UserNearbyResponse;

import java.util.List;

import static sogeun.backend.security.SecurityUtil.extractUserId;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sse/location")
public class LocationController {
    private final LocationService locationService;
    private final UserService userService;

    @PostMapping("/update")
    public ResponseEntity<Void> updateLocation(
            Authentication authentication,
            @RequestBody @Valid UpdateLocationRequest request
    ) {
        Long userId = extractUserId(authentication);
        locationService.saveLocation(userId, request.getLat(), request.getLon());
        return ResponseEntity.ok().build();
    }
    //이건 기존 연결로 대체
    // @GetMapping("/subscribe") // public SseEmitter subscribe(@RequestParam Long userId) { // return locationService.subscribe(userId); // } // //이거는 song때문에 잠시 주석..

    @GetMapping("/nearby")
    public List<UserNearbyResponse> nearby(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return userService.findNearbyBroadcastingUsers(userId);
    }

}
