package com.rebay.rebay_backend.payment.entity;

public enum TransactionStatus {
    PAYMENT_PENDING,     // 결제 대기
    READY, // 결제 시도
    PAID, // 상품 결제 완료
    SETTLEMENT_PENDING, // 상품 거래 완료 전 (정산 대기)
    COMPLETED, // 상품 거래 완료 (정산)
    CANCELED, // 거래 취소
    EXPIRED // 거래 만료
}
