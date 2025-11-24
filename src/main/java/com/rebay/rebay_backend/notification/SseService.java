package com.rebay.rebay_backend.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
public class SseService {

    private static final long TIMEOUT = 60L * 1000 * 60; // 1시간
    private final EmitterRepository repo;

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);

        repo.save(userId, emitter);

        emitter.onCompletion(() -> repo.delete(userId));
        emitter.onTimeout(() -> repo.delete(userId));
        emitter.onError((e) -> repo.delete(userId));

        // 연결 확인용
        sendTo(userId, "connected");

        return emitter;
    }

    public void sendTo(Long userId, Object data) {
        SseEmitter emitter = repo.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(data));
            } catch (Exception e) {
                repo.delete(userId);
            }
        }
    }

    // 경매방 별 접속자 관리
    private final Map<Long, List<SseEmitter>> auctionEmitters = new ConcurrentHashMap<>();

    // 경매방 입장 (구독)
    public SseEmitter subscribeAuction(Long auctionId) {
        SseEmitter emitter = new SseEmitter(60L * 1000 * 60); // 1시간 타임아웃

        // 해당 경매방 리스트에 접속자 추가 (없으면 생성)
        auctionEmitters.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // 연결 종료 시 리스트에서 제거
        emitter.onCompletion(() -> removeAuctionEmitter(auctionId, emitter));
        emitter.onTimeout(() -> removeAuctionEmitter(auctionId, emitter));
        emitter.onError((e) -> removeAuctionEmitter(auctionId, emitter));

        // 연결 성공 더미 데이터 전송 (연결 확인용)
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected to auction " + auctionId));
        } catch (IOException e) {
        }
        return emitter;
    }

    // 경매방 전체 방송 (입찰 발생 시 호출)
    public void broadcastToAuction(Long auctionId, Object data) {
        List<SseEmitter> emitters = auctionEmitters.get(auctionId);
        if (emitters != null) {
            emitters.forEach(emitter -> {
                try {
                    // "BID_UPDATE"라는 이름의 이벤트 전송
                    emitter.send(SseEmitter.event().name("BID_UPDATE").data(data));
                } catch (Exception e) {
                    removeAuctionEmitter(auctionId, emitter);
                }
            });
        }
    }

    private void removeAuctionEmitter(Long auctionId, SseEmitter emitter) {
        List<SseEmitter> emitters = auctionEmitters.get(auctionId);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }
}