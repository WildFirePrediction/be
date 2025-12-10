package com.capstone25.WildFirePrediction.sse;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class FireSseEmitterRepository {
    // 간단하게 클라이언트별 emitter를 관리 (key는 필요하면 세션/토큰 등으로 교체)
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // 구독 등록
    public SseEmitter add(String id, Long timeout) {
        SseEmitter emitter = new SseEmitter(timeout);
        emitters.put(id, emitter);

        emitter.onCompletion(() -> {
            log.info("SSE completed - id: {}", id);
            emitters.remove(id);
        });
        emitter.onTimeout(() -> {
            log.info("SSE timeout - id: {}", id);
            emitters.remove(id);
        });
        emitter.onError((e) -> {
            log.warn("SSE error - id: {}, error: {}", id, e.getMessage());
            emitters.remove(id);
        });

        return emitter;
    }

    // 전체 브로드캐스트
    public void sendToAll(Object data, String eventName) {
        emitters.forEach((id, emitter) -> {
            try {
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                        .name(eventName)
                        .data(data);
                emitter.send(event);
            } catch (IOException e) {
                log.warn("SSE send 실패 - id: {}, error: {}", id, e.getMessage());
                emitter.completeWithError(e);
            }
        });
    }
}
