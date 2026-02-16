package sogeun.backend.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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


    @GetMapping("/sse/stream")
    public SseEmitter connect(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("[SSE-CONNECT] userId={} connect request", userId);

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        registry.addOrReplace(userId, emitter);

        emitter.onCompletion(() -> registry.remove(userId));
        emitter.onTimeout(() -> registry.remove(userId));
        emitter.onError(e -> registry.remove(userId));

        return emitter;
    }

}
