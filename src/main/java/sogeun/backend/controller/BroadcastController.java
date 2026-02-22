package sogeun.backend.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sogeun.backend.service.BroadcastService;
import sogeun.backend.dto.request.BroadcastChangeMusicRequest;
import sogeun.backend.dto.request.BroadcastOnRequest;
import sogeun.backend.dto.response.MyBroadcastResponse;

import static sogeun.backend.security.SecurityUtil.extractUserId;

@Slf4j
@RestController
@RequestMapping("/api/broadcast")
public class BroadcastController {

    private final BroadcastService broadcastService;

    public BroadcastController(BroadcastService broadcastService) {
        this.broadcastService = broadcastService;
    }

    @PostMapping("/on")
    public ResponseEntity<Void> on(Authentication authentication,
                                   @RequestBody @Valid BroadcastOnRequest req) {
        log.info("[broadcast/on] req={}", req);
        Long userId = extractUserId(authentication);
        broadcastService.turnOn(userId, req.getLat(), req.getLon(), req.getMusic());
        return ResponseEntity.ok().build();
    }


    @PostMapping("/off")
    public ResponseEntity<Void> off(Authentication authentication) {
        Long userId = extractUserId(authentication);
        broadcastService.turnOff(userId);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/{broadcastId}/likes")
    public ResponseEntity<Void> like(
            @PathVariable Long broadcastId,
            Authentication authentication
    ) {
        Long likerUserId = extractUserId(authentication);
        broadcastService.like(broadcastId, likerUserId);
        return ResponseEntity.ok().build();
    }

    //방송 음악 변경
    @PostMapping("/changemusic")
    public ResponseEntity<Void> changeMusic(
            Authentication authentication,
            @RequestBody @Valid BroadcastChangeMusicRequest request
    ) {
        Long userId = extractUserId(authentication);

        broadcastService.changeMusic(userId, request.getMusic());

        return ResponseEntity.ok().build();
    }

    //현재 자기 방송 정보 조회
    @GetMapping("/me")
    public ResponseEntity<MyBroadcastResponse> me(Authentication authentication) {
        Long userId = extractUserId(authentication);
        MyBroadcastResponse res = broadcastService.getMyBroadcast(userId);
        return ResponseEntity.ok(res);
    }

}
