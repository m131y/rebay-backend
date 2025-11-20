package com.rebay.rebay_backend.payment.util;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

// 토스페이먼츠 웹훅 서명 검증 유틸리티
// HMAC-SHA256 알고리즘을 사용하여 웹훅 요청의 무결성을 검증합니다

@Component
@Slf4j
public class TossWebhookSignature {
    private static final String HMAC_SHA256 = "HmacSHA256";

    // 토스페이먼츠 웹훅 서명 검증
    // @param payload 웹훅 요청 본문 (JSON)
    // @param signature 토스에서 제공한 서명 (Toss-Signature 헤더 값)
    // @param secretKey 토스페이먼츠 Secret Key
    // @return 서명이 유효하면 true, 아니면 false
    public boolean validateSignature(String payload, String signature, String secretKey) {
        if (payload == null || signature == null || secretKey == null) {
            log.warn("웹훅 서명 검증 실패: null 값 존재");
            return false;
        }

        try {
            String calculatedSignature = calculateSignature(payload, secretKey);
            boolean isVaild = calculatedSignature.equals(signature);

            if (!isVaild) {
                log.warn("웹훅 서명 불일치. Expected: {}, Actual: {}", calculatedSignature, signature);
            }

            return isVaild;

        } catch (Exception e) {
            log.error("웹훅 서명 검증 중 오류 발생", e);
            return false;
        }
    }

    // HMAC-SHA256을 사용하여 서명 생성
    // @param payload 웹훅 요청 본문
    // @param secretKey Secret Key
    // @return Base64 인코딩된 서명
    private String calculateSignature(String payload, String secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
        // 예외처리 필수
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256
        );
        mac.init(secretKeySpec);

        byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }
}
