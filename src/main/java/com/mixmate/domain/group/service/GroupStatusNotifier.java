package com.mixmate.domain.group.service;

import com.mixmate.domain.group.enums.GroupStatus;
import com.mixmate.domain.group.event.GroupStatusChangedEvent;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GroupStatusNotifier {

    private static final long TIMEOUT = 1000L * 60 * 30;    // 30분
    private static final String EVENT_NAME = "status";
    // 클라이언트 라이브러리의 유휴 타임아웃(가장 짧은 것이 45초)보다 짧아야 멀쩡한 연결이 끊기지 않는다.
    private static final long HEARTBEAT_INTERVAL = 20_000;

    private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long groupId, GroupStatus currentStatus) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);

        emitter.onCompletion(() -> remove(groupId, emitter));
        emitter.onTimeout(() -> remove(groupId, emitter));
        emitter.onError(e -> remove(groupId, emitter));

        emitters.computeIfAbsent(groupId, key -> ConcurrentHashMap.newKeySet()).add(emitter);

        // 연결 직후 현재 상태를 한 번 보낸다. 끊겼다가 다시 붙는 사이에 놓친 변경을 메워준다.
        send(groupId, emitter, statusEvent(new GroupStatusChangedEvent(groupId, currentStatus)));

        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyStatusChanged(GroupStatusChangedEvent event) {
        Set<SseEmitter> targets = emitters.get(event.groupId());
        if (targets == null) return;

        targets.forEach(emitter -> send(event.groupId(), emitter, statusEvent(event)));
    }

    @Scheduled(fixedRate = HEARTBEAT_INTERVAL)
    public void heartbeat() {
        emitters.forEach((groupId, targets) ->
                targets.forEach(emitter -> send(groupId, emitter, SseEmitter.event().comment("ping"))));
    }

    private SseEmitter.SseEventBuilder statusEvent(GroupStatusChangedEvent event) {
        return SseEmitter.event().name(EVENT_NAME).data(event, MediaType.APPLICATION_JSON);
    }

    private void send(Long groupId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            remove(groupId, emitter);
        }
    }

    private void remove(Long groupId, SseEmitter emitter) {
        emitters.computeIfPresent(groupId, (key, targets) -> {
            targets.remove(emitter);
            return targets.isEmpty() ? null : targets;
        });
    }
}
