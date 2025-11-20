package com.rebay.rebay_backend.notification;

import com.rebay.rebay_backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SseService sseService;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    // SSE 구독
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication auth) {
        User user = (User) auth.getPrincipal(); // Principal을 User로 캐스팅
        return sseService.subscribe(user.getId());
    }

    // 목록 조회
    @GetMapping
    public List<Notification> list(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return notificationRepository.findTop30ByReceiverIdOrderByCreatedAtDesc(user.getId());
    }

    // 미읽음 개수
    @GetMapping("/count")
    public long count(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return notificationRepository.countByReceiverIdAndReadIsFalse(user.getId());
    }

    // 읽음 처리
    @PostMapping("/{id}/read")
    public void read(@PathVariable Long id, Authentication auth) {
        User user = (User) auth.getPrincipal();
        notificationService.markRead(id, user.getId());
    }

    // ====== 개발 테스트용 ======

    // 인증 없이 userId로 바로 구독
    @GetMapping(value = "/dev/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter devStream(@RequestParam Long userId) {
        return sseService.subscribe(userId);
    }

    // 임시로 알림 보내기
    @PostMapping("/dev/send")
    public void devSend(@RequestParam Long to,
                        @RequestParam(defaultValue = "hello") String msg) {
        notificationService.notify(to, "DEV", msg, "/");
    }
}