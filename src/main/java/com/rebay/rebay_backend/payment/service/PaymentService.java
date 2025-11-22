package com.rebay.rebay_backend.payment.service;

import com.rebay.rebay_backend.Post.entity.Post;
import com.rebay.rebay_backend.Post.entity.SaleStatus;
import com.rebay.rebay_backend.Post.repository.PostRepository;
import com.rebay.rebay_backend.payment.config.TossPaymentConfig;
import com.rebay.rebay_backend.payment.dto.PaymentRequest;
import com.rebay.rebay_backend.payment.dto.TossPaymentRequest;
import com.rebay.rebay_backend.payment.dto.TossPaymentResponse;
import com.rebay.rebay_backend.payment.dto.TransactionResponse;
import com.rebay.rebay_backend.payment.entity.Payment;
import com.rebay.rebay_backend.payment.entity.Transaction;
import com.rebay.rebay_backend.payment.entity.TransactionStatus;
import com.rebay.rebay_backend.payment.repository.PaymentRepository;
import com.rebay.rebay_backend.payment.repository.TransactionRepository;
import com.rebay.rebay_backend.user.entity.User;
import com.rebay.rebay_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final TossPaymentsApiClient tossPaymentsApiClient;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final TossPaymentConfig tossPaymentConfig;


    // Transaction 재사용
    private Transaction findOrCreateTransaction(Post post, User buyer, User seller) {
        Transaction transaction = transactionRepository.findActiveTransaction(post.getId(), buyer.getId())
                .orElse(null);

        // 기존 거래 조회
        if (transaction != null) {
            log.info("[Transaction] 기존 거래 발견: id={}, status={}", transaction.getId(), transaction.getStatus());

            // 만료 확인
            if (transaction.isExpired()) {
                transaction.expirePayment();
                transactionRepository.save(transaction);
                log.info("[Transaction] 만료된 거래 -> EXPIRED로 업데이트: id={}", transaction.getId());
            } else if (transaction.getStatus() == TransactionStatus.PAYMENT_PENDING ||
                    transaction.getStatus() == TransactionStatus.READY) {
                // PAYMENT_PENDING / READY 상태면 재사용
                log.info("[Transaction] 기존 거래 재사용: id={}", transaction.getId());
                return transaction;
            }
        }

        // 새 거래 생성
        Transaction newTransaction = Transaction.builder()
                .post(post)
                .buyer(buyer)
                .seller(seller)
                .status(TransactionStatus.PAYMENT_PENDING)
                .isReceived(false)
                .build();

        transactionRepository.save(newTransaction);
        log.info("[Transaction] 새 거래 생성: id={}, status={}", newTransaction.getId(), newTransaction.getStatus());


        return newTransaction;

    }

    // 결제 준비 : Transaction, Payment 생성
    public TransactionResponse preparePayment(PaymentRequest request) {
        // 거래 게시글 조회
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다." + request.getPostId()));

        // 거래 게시글 상태 확인
        if (post.getStatus() != SaleStatus.ON_SALE) {
            throw new IllegalStateException("판매 중인 상품이 아닙니다.");
        }

        // 구매자 조회
        User buyer = userRepository.findById(request.getBuyerId())
                .orElseThrow(() -> new IllegalArgumentException("구매자를 찾을 수 없습니다: " + request.getBuyerId()));

        // 판매 요청자가 판매자인지 확인
        User seller = post.getUser();
        if (seller.getId().equals(buyer.getId())) {
            throw new IllegalArgumentException("자신의 상품은 구매할 수 없습니다.");
        }

        Transaction transaction = findOrCreateTransaction(post, buyer, seller);

        // 상태 변경
        transaction.readyPayment();
        // transactionRepository.save(transaction);


        Payment existingPayment = paymentRepository.findByTransactionId(transaction.getId())
                .orElse(null);

        if (existingPayment != null) {   // Payment 재사용
            log.info("Payment 재사용: orderId={}, transactionId={}",
                    existingPayment.getOrderId(), transaction.getId());
            return toTransactionResponse(transaction, existingPayment.getOrderId());
        }

        // 안전결제 생성
        String orderId = generateOrderId();
        Payment payment = Payment.create(transaction, orderId, post.getPrice());
        paymentRepository.save(payment);

        // 결제 준비 완료
        log.info("준비 완료: orderId={}, amount={}", orderId, post.getPrice(), post.getId());

        return toTransactionResponse(transaction, orderId);
    }

    // 결제 승인
    @Transactional
    public TransactionResponse confirmPayment(TossPaymentRequest request) {

        // 결제 조회
        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));

        // 금액 검증
        if (payment.getAmount().compareTo(BigDecimal.valueOf(request.getAmount())) != 0) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
        }

        try {
            // 토스페이먼츠 결제 승인
            TossPaymentResponse tossPaymentResponse = tossPaymentsApiClient.confirmPayment(request);

            // 중고거래 결제 승인
            payment.approve(
                    tossPaymentResponse.getPaymentKey(),
                    tossPaymentResponse.getMethod(),
                    tossPaymentResponse.getReceipt() != null ? tossPaymentResponse.getReceipt().getUrl() : null
            );
            paymentRepository.save(payment);

            // 거래 상태 업데이트 - 결제 완료(결제 금액 예치)
            Transaction transaction = payment.getTransaction();
            transaction.confirmPayment();

            // 게시글 상태 업데이트
            transaction.getPost().setStatus(SaleStatus.SOLD);

            log.info("결제 승인 완료 (결제 금액 예치): paymentKey={}, orderId={}", request.getPaymentKey(), request.getOrderId());

            return toTransactionResponse(transaction, request.getOrderId());

        } catch (Exception e) {
            log.error("결제 승인 실패: paymentKey={}, error={}", request.getPaymentKey(), e.getMessage());
            throw new RuntimeException("결제 승인에 실패했습니다: " + e.getMessage());
        }

    }

    // 상품 수령 확인
    public TransactionResponse confirmReceipt(Long transactionId, Long buyerId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다." + transactionId));

        // 실구매자 일치 여부 확인
        if (!transaction.getBuyer().getId().equals(buyerId)) {
            throw new IllegalArgumentException("구매자만 상품 수령을 확인할 수 있습니다.");
        }

        // 결제 상태 확인
        if (transaction.getStatus() != TransactionStatus.PAID) {
            throw new IllegalArgumentException("결제가 완료된 거래만 수령 확인이 가능합니다");
        }

        // 상품 수령 확인
        transaction.confirmReceipt();
        //transactionRepository.save(transaction);

        log.info("상품 수령 확인: transactionId={}, buyerId={}", transactionId, buyerId);

        // 판매자에게 예치금 정산
        return settlementToSeller(transaction);

    }

    // 판매자 정산 - 거래 완료 후 판매자에게 예치금 전달
    private TransactionResponse settlementToSeller(Transaction transaction) {
        // 거래 상태 확인
        if (transaction.getStatus() != TransactionStatus.SETTLEMENT_PENDING) {
            throw new IllegalArgumentException("정산 대기 상태가 아닙니다.");
        }

        // 안전결제 조회
        Payment payment = paymentRepository.findByTransactionId(transaction.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 결제를 찾을 수 없습니다."));

        // 안전결제 예치금 정산 처리
        payment.settle();
        transaction.completeSettlement();

        // 판매자 적립금 업데이트
        transaction.getSeller().addPoints(payment.getAmount());

        log.info("판매자 정산 완료: transactionId={}, sellerId={}, amount={}, totalEarnings={}",
                transaction.getId(), transaction.getSeller().getId(), payment.getAmount(), transaction.getSeller().getTotalPoints());

        return toTransactionResponse(transaction, payment.getOrderId());
    }

    // 거래 조회
    public TransactionResponse getTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다: " + transactionId));

        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("해당 안전결제를 찾을 수 없습니다."));

        return toTransactionResponse(transaction, payment.getOrderId());
    }

    public Page<TransactionResponse> getTransactionsByBuyerId(Long buyerId, Pageable pageable) {
        Page<Transaction> transactions = transactionRepository.findByBuyerId(buyerId, defaultSorting(pageable));

        // 만료 처리 + DB 반영
        transactions.forEach(t -> {
            if ((t.getStatus() == TransactionStatus.PAYMENT_PENDING ||
                    t.getStatus() == TransactionStatus.READY) && t.isExpired()) {
                t.expirePayment();
                log.info("[Transaction] 만료 처리: id={}", t.getId());
            }
        });


        // Payment를 Transaction 마다 개별 조회(N+1) -> Payment 한 번에 조회로 변경 (IN 조회)
        List<Long> transactionIds = transactions.map(Transaction::getId).toList();
        Map<Long, Payment> paymentMap = paymentRepository.findByTransactionIdIn(transactionIds)
                .stream()
                .collect(Collectors.toMap(p -> p.getTransaction().getId(), p -> p));
        return transactions.map(t -> {
            Payment payment = paymentMap.get(t.getId());
            return toTransactionResponse(t, payment != null ? payment.getOrderId() : null);
        });
    }

    public Page<TransactionResponse> getTransactionsBySellerId(Long sellerId, Pageable pageable) {
        Page<Transaction> transactions = transactionRepository.findBySellerId(sellerId, defaultSorting(pageable));

        // 만료 처리 + DB 반영
        transactions.forEach(t -> {
            if ((t.getStatus() == TransactionStatus.PAYMENT_PENDING ||
                    t.getStatus() == TransactionStatus.READY) && t.isExpired()) {
                t.expirePayment();
                log.info("[Transaction] 만료 처리: id={}", t.getId());
            }
        });

        List<Long> transactionIds = transactions.map(Transaction::getId).toList();
        Map<Long, Payment> paymentMap = paymentRepository.findByTransactionIdIn(transactionIds)
                .stream()
                .collect(Collectors.toMap(p -> p.getTransaction().getId(), p -> p));

        return transactions.map(t -> {
            Payment payment = paymentMap.get(t.getId());
            return toTransactionResponse(t, payment != null ? payment.getOrderId() : null);
        });
    }

    private TransactionResponse toTransactionResponse(Transaction transaction, String orderId) {
        Post post = transaction.getPost();
        User buyer = transaction.getBuyer();
        User seller = transaction.getSeller();

        // Lazy Loading 방지 -> EntityGraph로 로딩
//        post.getTitle();
//        buyer.getUsername();
//        seller.getUsername();

        return TransactionResponse.builder()
                .id(transaction.getId())
                .postId(post.getId())
                .productName(post.getTitle())
                .amount(post.getPrice())
                .buyerId(buyer.getId())
                .buyerName(buyer.getUsername())
                .sellerId(seller.getId())
                .sellerName(seller.getUsername())
                .isReceived(transaction.getIsReceived())
                .clientKey(tossPaymentConfig.getClientKey())
                .receivedAt(transaction.getReceivedAt())
                .status(transaction.getStatus())
                .orderId(orderId)
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private Pageable defaultSorting(Pageable pageable) {

        // 조건이 있다면 조건에 따라 정렬 (가격순, 상태순..)
        if (pageable.getSort().isSorted()) {
            return pageable;
        }

        // 기본 createdAt DESC (최신순) 정렬
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    private String generateOrderId() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String random = UUID.randomUUID().toString()
                .substring(0, 8)
                .toUpperCase();

        return "ORDER_" + timestamp + "_" + random;
    }

}
