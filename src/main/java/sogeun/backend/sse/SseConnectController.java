package sogeun.backend.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
public class SseConnectController {

    private static final Logger log =
            LoggerFactory.getLogger(SseConnectController.class);

    private final SseEmitterRegistry registry;

    public SseConnectController(SseEmitterRegistry registry) {
        this.registry = registry;
    }


    @GetMapping(value = "/api/sse/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName()); // 너 로직에 맞게

        log.info("[SSE-CONNECT] userId={} connect request", userId);

        SseEmitter emitter = new SseEmitter(0L); // 무제한(원하면 30분 등으로)
        log.info("[SSE-EMITTER] userId={} created", userId);

        emitter.onCompletion(() -> log.info("[SSE-DONE] userId={} completion", userId));
        emitter.onTimeout(() -> log.warn("[SSE-TIMEOUT] userId={} timeout", userId));
        emitter.onError(e -> log.warn("[SSE-ERROR] userId={} error={}", userId, e.toString(), e));

        try {
            emitter.send(SseEmitter.event().name("init").data("ok"));
            log.info("[SSE-SEND] userId={} init sent", userId);
        } catch (Exception e) {
            log.warn("[SSE-SEND-FAIL] userId={} fail={}", userId, e.toString(), e);
        }

        return emitter;
    }
}
