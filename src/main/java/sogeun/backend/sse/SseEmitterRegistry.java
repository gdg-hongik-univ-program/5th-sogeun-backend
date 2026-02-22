package sogeun.backend.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseEmitterRegistry {

    private static final Logger log =
            LoggerFactory.getLogger(SseEmitterRegistry.class);

    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    //같은 userId로 새 연결이 오면, 기존 emitter를 종료하고 새 emitter로 교체
    public void addOrReplace(Long userId, SseEmitter newEmitter) {
        SseEmitter old = emitters.put(userId, newEmitter);

        if (old != null && old != newEmitter) {
            log.info("[SSE-REPLACE] userId={} old emitter exists → complete old", userId);
            try {
                old.complete(); // 기존 연결 종료
            } catch (Exception ignore) {
                // 이미 종료된 경우 등
            }
        }
    }

    //연결 해제
    public void disconnect(Long userId) {
        SseEmitter emitter = emitters.remove(userId); // 먼저 제거

        if (emitter != null) {
            log.info("[SSE-DISCONNECT] userId={} complete emitter", userId);
            try {
                emitter.complete(); // 실제 연결 종료
            } catch (Exception ignore) {
            }
        }
    }

    public SseEmitter get(Long userId) {
        return emitters.get(userId);
    }

    public void remove(Long userId) {
        emitters.remove(userId);
    }

    public Iterable<Map.Entry<Long, SseEmitter>> entries() {
        return emitters.entrySet();
    }

    public int size() { return emitters.size(); }
}
