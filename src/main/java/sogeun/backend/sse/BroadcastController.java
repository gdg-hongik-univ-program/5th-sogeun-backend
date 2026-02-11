package sogeun.backend.sse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sogeun.backend.dto.request.BroadcastOnRequest;
import sogeun.backend.sse.dto.BroadcastLikeRequest;
import static sogeun.backend.security.SecurityUtil.extractUserId;
import java.nio.file.attribute.UserPrincipal;

@Slf4j
@RestController
public class BroadcastController {

    private final BroadcastService broadcastService;

    public BroadcastController(BroadcastService broadcastService) {
        this.broadcastService = broadcastService;
    }

    @PostMapping("/broadcast/on")
    public ResponseEntity<Void> on(Authentication authentication,
                                   @RequestBody @Valid BroadcastOnRequest req) {
        log.info("[broadcast/on] req={}", req);
        Long userId = extractUserId(authentication);
        broadcastService.turnOn(userId, req.getLat(), req.getLon(), req.getMusic());
        return ResponseEntity.ok().build();
    }


    @PostMapping("/broadcast/off")
    public ResponseEntity<Void> off(Authentication authentication) {
        Long userId = extractUserId(authentication);
        broadcastService.turnOff(userId);
        return ResponseEntity.ok().build();
    }


    // 방송 좋아요
    @PostMapping("/broadcast/like")
    public ResponseEntity<String> like(@RequestBody BroadcastLikeRequest request) {
        broadcastService.toggleLike(request);
        return ResponseEntity.ok("like toggled");
    }
}
