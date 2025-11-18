package com.rebay.rebay_backend.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rebay.rebay_backend.payment.config.TossPaymentConfig;
import com.rebay.rebay_backend.payment.dto.WebhookRequest;
import com.rebay.rebay_backend.payment.service.WebhookService;
import com.rebay.rebay_backend.payment.util.TossWebhookSignatureValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookService webhookService;
    private final TossWebhookSignatureValidator signatureValidator;
    private final TossPaymentConfig tossPaymentConfig;

    @PostMapping("/toss")
    public ResponseEntity<Void> handleTossWebhook(
            @RequestHeader(value = "Toss-Signature", required = false) String signature,
            @RequestBody String payload
    ) {
        // JSON -> 객체 변환
        WebhookRequest webhookRequest = null;
        try {
            webhookRequest = new ObjectMapper().readValue(payload, WebhookRequest.class);
        } catch (Exception e) {
            log.error("웹훅 파싱 실패: {}", e.getMessage());
            return ResponseEntity.ok().build(); // 200으로 응답(재시도 방지)
        }

        log.info("웹훅 수신: eventType={}, orderId={}",
                webhookRequest.getEventType(),
                webhookRequest.getData().getOrderId());

        // 서명 검증 - 테스트 환경에서는 서명이 제공되지 않을 수 있음
        if (signature != null) {
            boolean isValid = signatureValidator.validateSignature(
                    payload,
                    signature,
                    tossPaymentConfig.getSecretKey()
            );

            if (!isValid) {
                log.error("웹훅 서명 검증 실패: orderId={}", webhookRequest.getData().getOrderId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } else {
            log.warn("웹훅 서명이 제공되지 않음 (테스트 환경일 수 있음)");
        }

        try {
            // 웹훅 이벤트 처리 (멱등성 보장)
            webhookService.processWebhookEvent(payload, webhookRequest);

            // 토스페이먼츠는 200 OK 응답을 받아야 성공으로 처리
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("웹훅 처리 중 오류 발생: orderId={}, error={}",
                    webhookRequest.getData().getOrderId(), e.getMessage(), e);

            // 에러가 발생해도 200 OK를 반환 (이미 로그에 기록됨)
            // 500 에러를 반환하면 토스가 재시도를 계속하므로 주의!!
            return ResponseEntity.ok().build();
        }
    }

    @PostMapping("/toss/test")
    public ResponseEntity<String> testWebhook(@RequestBody WebhookRequest webhookRequest) {
        log.info("테스트 웹훅 수신: eventType={}, orderId={}",
                webhookRequest.getEventType(),
                webhookRequest.getData().getOrderId());

        try {
            // 서명 검증 없이 직접 처리
            webhookService.processWebhookEvent(
                    "test-payload",
                    webhookRequest
            );

            return ResponseEntity.ok("웹훅 테스트 성공");
        } catch (Exception e) {
            log.error("웹훅 테스트 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("웹훅 테스트 실패: " + e.getMessage());
        }
    }

}
