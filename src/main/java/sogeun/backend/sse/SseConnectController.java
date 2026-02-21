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


    @GetMapping(value = "/sse/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());

        log.info("[SSE-CONNECT] userId={} connect request", userId);

        SseEmitter emitter = new SseEmitter(0L);
        log.info("[SSE-EMITTER] userId={} created", userId);

        registry.addOrReplace(userId, emitter);
        log.info("[SSE-REGISTRY] userId={} registered size={}", userId, registry.size());

        emitter.onCompletion(() -> {
            registry.remove(userId);
            log.info("[SSE-DONE] userId={} completion (removed) size={}", userId, registry.size());
        });
        emitter.onTimeout(() -> {
            registry.remove(userId);
            log.warn("[SSE-TIMEOUT] userId={} timeout (removed) size={}", userId, registry.size());
        });
        emitter.onError(e -> {
            registry.remove(userId);
            log.warn("[SSE-ERROR] userId={} error={} (removed) size={}", userId, e.toString(), registry.size(), e);
        });

        try {
            emitter.send(SseEmitter.event().name("init").data("ok"));
            log.info("[SSE-SEND] userId={} init sent", userId);
        } catch (Exception e) {
            log.warn("[SSE-SEND-FAIL] userId={} fail={}", userId, e.toString(), e);
        }

        return emitter;
    }
}
