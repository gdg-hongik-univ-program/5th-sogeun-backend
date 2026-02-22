package sogeun.backend.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Component
public class SseHeartbeatScheduler {

    //sse는 주기적인 핑이 있어야 안 끊긴대서 만들었어요 -> 저희 메인 기능과는 관련없으니 무시하시면 됩니당

    private static final Logger log =
            LoggerFactory.getLogger(SseHeartbeatScheduler.class);

    private final SseEmitterRegistry registry;

    public SseHeartbeatScheduler(SseEmitterRegistry registry) {
        this.registry = registry;
    }

    // 30초마다 ping 보냄
    @Scheduled(fixedRate = 30_000)
    public void pingAll() {
        int total = 0;        int success = 0;
        int failed = 0;

        for (Map.Entry<Long, SseEmitter> entry : registry.entries()) {
            total++;
            Long userId = entry.getKey();
            SseEmitter emitter = entry.getValue();

            try {
                emitter.send(
                        SseEmitter.event()
                                .name("ping")
                                .data("keep-alive")
                );
                success++;
                log.debug("[SSE-PING] success userId={}", userId);

            } catch (IOException | IllegalStateException e) {
                failed++;
                log.warn("[SSE-PING] failed userId={} → remove emitter", userId);
                registry.remove(userId);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignore) {}
            }
        }

        log.info(
                "[SSE-PING] finished total={} success={} failed={}",
                total, success, failed
        );
    }
}
