package com.company.oa.message;

import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/notifications")
public class NotificationSseController {
    private static final Logger log = LoggerFactory.getLogger(NotificationSseController.class);
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    private final AuthService authService;

    public NotificationSseController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        AuthUser user = authService.currentUser();
        long userId = user.id();
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        CopyOnWriteArrayList<SseEmitter> emitters = userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("userId", userId)));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    public void notifyUser(long userId, String type, String title, String content) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null) return;
        Iterator<SseEmitter> it = emitters.iterator();
        while (it.hasNext()) {
            SseEmitter emitter = it.next();
            try {
                emitter.send(SseEmitter.event().name("notification").data(Map.of(
                    "type", type, "title", title, "content", content,
                    "userId", userId, "timestamp", java.time.LocalDateTime.now().toString()
                )));
            } catch (IOException e) {
                log.warn("SSE emitter send failed, removing: {}", e.getMessage());
                it.remove();
            }
        }
    }
}
