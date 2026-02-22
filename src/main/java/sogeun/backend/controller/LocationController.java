package sogeun.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sogeun.backend.service.LocationService;
import sogeun.backend.dto.request.UpdateLocationRequest;
import sogeun.backend.service.UserService;
import sogeun.backend.dto.response.UserNearbyResponse;

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

    @GetMapping("/nearby")
    public List<UserNearbyResponse> nearby(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return userService.findNearbyBroadcastingUsers(userId);
    }

}
