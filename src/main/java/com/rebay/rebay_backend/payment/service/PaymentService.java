package com.rebay.rebay_backend.payment.service;

import com.rebay.rebay_backend.Post.entity.Post;
import com.rebay.rebay_backend.Post.entity.SaleStatus;
import com.rebay.rebay_backend.Post.repository.PostRepository;
import com.rebay.rebay_backend.auction.entity.Auction;
import com.rebay.rebay_backend.auction.repository.AuctionRepository;
import com.rebay.rebay_backend.payment.config.TossPaymentConfig;
import com.rebay.rebay_backend.payment.dto.PaymentRequest;
import com.rebay.rebay_backend.payment.dto.TossPaymentRequest;
import com.rebay.rebay_backend.payment.dto.TossPaymentResponse;
import com.rebay.rebay_backend.payment.dto.TransactionResponse;
import com.rebay.rebay_backend.payment.entity.*;
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
    private final AuctionRepository auctionRepository;


    // Transaction 재사용 또는 생성
    private Transaction findOrCreateTransaction(Post post, Auction auction, User buyer, User seller, TransactionType type) {
        Transaction transaction = transactionRepository.findActiveTransaction(
                post != null ? post.getId() : auction.getId(),
                buyer.getId()
        ).orElse(null);

        // 기존 거래가 있는 경우 상태 확인 후 재사용 또는 만료 처리
        if (transaction != null) {
            if (transaction.isExpired()) {
                transaction.expirePayment();
                log.info("[Transaction] 만료 처리: id={}", transaction.getId());
            } else if (isReusableStatus(transaction.getStatus())) {
                log.info("[Transaction] 기존 거래 재사용: id={}", transaction.getId());
                return transaction;
            }
        }

        // 새 거래 생성
        Transaction.TransactionBuilder builder = Transaction.builder()
                .buyer(buyer)
                .seller(seller)
                .transactionType(type)
                .status(TransactionStatus.PAYMENT_PENDING)
                .isReceived(false);

        if (type == TransactionType.AUCTION) {
            builder.auction(auction).auctionStatus(AuctionStatus.WON);
        } else {
            builder.post(post);
        }

        Transaction newTransaction = builder.build();
        transactionRepository.save(newTransaction);

        log.info("[Transaction] 새 거래 생성: id={}, type={}", newTransaction.getId(), type);

        return newTransaction;
    }

    // 결제 준비 : Transaction, Payment 생성 (일반/경매 구분 처리)
    public TransactionResponse preparePayment(PaymentRequest request) {

        // 경매인지 확인
        Auction auction = auctionRepository.findById(request.getPostId()).orElse(null);

        if (auction != null) {
            return prepareAuctionPayment(request, auction);
        }

        return prepareDefaultPayment(request);
    }

    // 일반 결제 준비
    private TransactionResponse prepareDefaultPayment(PaymentRequest request) {
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + request.getPostId()));

        validatePostForSale(post);

        User buyer = getBuyer(request.getBuyerId());
        User seller = post.getUser();

        validateBuyerNotSeller(buyer, seller);

        // 거래 조회 또는 생성
        Transaction transaction = findOrCreateTransaction(post, null, buyer, seller, TransactionType.DEFAULT);
        transaction.readyPayment();

        // Payment 생성 또는 재사용
        String orderId = getOrCreateOrderId(transaction);

        log.info("[DEFAULT] 결제 준비 완료: orderId={}, postId={}", orderId, post.getId());

        return toTransactionResponse(transaction, orderId);
    }

    // 경매 결제 준비
    private TransactionResponse prepareAuctionPayment(PaymentRequest request, Auction auction) {
        User buyer = getBuyer(request.getBuyerId());
        User seller = auction.getSeller();

        // 낙찰된 거래 찾기
        Transaction transaction = transactionRepository
                .findByAuctionAndBuyerAndAuctionStatus(auction, buyer, AuctionStatus.WON)
                .orElseThrow(() -> new IllegalStateException("낙찰자만 결제를 시작할 수 있습니다."));

        transaction.readyPayment();

        // Payment 생성 또는 재사용
        String orderId = getOrCreateOrderId(transaction);

        log.info("[AUCTION] 결제 준비 완료: orderId={}, auctionId={}", orderId, auction.getId());

        return toTransactionResponse(transaction, orderId);
    }


    // 결제 승인 처리
    @Transactional
    public TransactionResponse confirmPayment(TossPaymentRequest request) {
        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));

        validatePaymentAmount(payment, request.getAmount());

        try {
            // 토스 결제 승인 API 호출
            TossPaymentResponse tossResponse = tossPaymentsApiClient.confirmPayment(request);

            // 결제 정보 업데이트
            payment.approve(
                    tossResponse.getPaymentKey(),
                    tossResponse.getMethod(),
                    tossResponse.getReceipt() != null ? tossResponse.getReceipt().getUrl() : null
            );

            // 거래 상태 업데이트
            Transaction transaction = payment.getTransaction();
            transaction.confirmPayment();

            updateItemStatus(transaction);

            log.info("[PAYMENT] 결제 승인 완료: paymentKey={}, orderId={}",
                    request.getPaymentKey(), request.getOrderId());

            return toTransactionResponse(transaction, request.getOrderId());

        } catch (Exception e) {
            log.error("[PAYMENT] 결제 승인 실패: paymentKey={}, error={}",
                    request.getPaymentKey(), e.getMessage());
            throw new RuntimeException("결제 승인에 실패했습니다: " + e.getMessage());
        }
    }

    // 상품 수령 확인
    public TransactionResponse confirmReceipt(Long transactionId, Long buyerId) {
        Transaction transaction = transactionRepository.findWithDetailsById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다: " + transactionId));

        validateBuyerForReceipt(transaction, buyerId);
        validateTransactionStatus(transaction, TransactionStatus.PAID);

        transaction.confirmReceipt();

        log.info("[RECEIPT] 상품 수령 확인: transactionId={}, buyerId={}", transactionId, buyerId);

        return settlementToSeller(transaction);
    }

    // 판매자 정산 - 거래 완료 후 판매자에게 예치금 전달
    private TransactionResponse settlementToSeller(Transaction transaction) {
        validateTransactionStatus(transaction, TransactionStatus.SETTLEMENT_PENDING);

        Payment payment = paymentRepository.findByTransactionId(transaction.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 결제를 찾을 수 없습니다."));

        // 정산 완료 처리
        payment.settle();
        transaction.completeSettlement();

        // 판매자 예치금 정산
        User seller = transaction.getSeller();
        seller.addPoints(payment.getAmount());

        log.info("[SETTLEMENT] 판매자 정산 완료: transactionId={}, sellerId={}, amount={}",
                transaction.getId(), seller.getId(), payment.getAmount());

        return toTransactionResponse(transaction, payment.getOrderId());
    }

    // 거래 조회
    public TransactionResponse getTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다: " + transactionId));

        boolean isAuctionExpired =
                transaction.getTransactionType() == TransactionType.AUCTION
                        && transaction.getStatus() == TransactionStatus.EXPIRED;

        if (isAuctionExpired) {
            return toTransactionResponse(transaction, null);
        }

        Payment payment = paymentRepository.findByTransactionId(transactionId).orElse(null);

        String orderId = (payment != null ? payment.getOrderId() : null);

        return toTransactionResponse(transaction, orderId);
    }

    // 구매자 거래 목록 조회
    public Page<TransactionResponse> getTransactionsByBuyerId(Long buyerId, Pageable pageable) {
        return getTransactionsByUserId(
                transactionRepository.findByBuyerId(buyerId, defaultSorting(pageable))
        );
    }

    // 판매자 거래 목록 조회
    public Page<TransactionResponse> getTransactionsBySellerId(Long sellerId, Pageable pageable) {
        return getTransactionsByUserId(
                transactionRepository.findBySellerId(sellerId, defaultSorting(pageable))
        );
    }

    private String getOrCreateOrderId(Transaction transaction) {
        Payment existingPayment = paymentRepository.findByTransactionId(transaction.getId()).orElse(null);

        if (existingPayment != null) {
            log.info("[Payment] 기존 Payment 재사용: orderId={}", existingPayment.getOrderId());
            return existingPayment.getOrderId();
        }

        String orderId = generateOrderId(transaction.getTransactionType());
        BigDecimal amount = getTransactionAmount(transaction);

        Payment payment = Payment.create(transaction, orderId, amount);
        paymentRepository.save(payment);

        log.info("[Payment] 신규 Payment 생성: orderId={}, amount={}", orderId, amount);

        return orderId;
    }

    // 거래 목록 변환 + 만료 처리 + Payment 매핑
    private Page<TransactionResponse> getTransactionsByUserId(Page<Transaction> transactions) {
        // 만료 처리
        transactions.forEach(this::expireIfNeeded);

        // Payment 일괄 조회 (N+1 방지)
        List<Long> transactionIds = transactions.map(Transaction::getId).toList();
        Map<Long, Payment> paymentMap = paymentRepository.findByTransactionIdIn(transactionIds)
                .stream()
                .collect(Collectors.toMap(p -> p.getTransaction().getId(), p -> p));

        return transactions.map(t -> {
            Payment payment = paymentMap.get(t.getId());
            return toTransactionResponse(t, payment != null ? payment.getOrderId() : null);
        });
    }

    // Transaction 만료 처리
    private void expireIfNeeded(Transaction transaction) {
        if (isReusableStatus(transaction.getStatus()) && transaction.isExpired()) {
            transaction.expirePayment();
            log.info("[Transaction] 만료 처리: id={}", transaction.getId());
        }
    }

    private TransactionResponse toTransactionResponse(Transaction transaction, String orderId) {
        User buyer = transaction.getBuyer();
        User seller = transaction.getSeller();

        ItemInfo itemInfo = getItemInfo(transaction);

        return TransactionResponse.builder()
                .id(transaction.getId())
                .postId(itemInfo.id)
                .productName(itemInfo.name)
                .amount(itemInfo.amount)
                .transactionType(transaction.getTransactionType())
                .auctionStatus(transaction.getAuctionStatus())
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

    // 거래 상품 정보 조회 (일반/경매 구분)
    private ItemInfo getItemInfo(Transaction transaction) {
        if (transaction.getTransactionType() == TransactionType.AUCTION) {
            Auction auction = transaction.getAuction();
            if (auction == null) {
                throw new IllegalStateException("경매 정보를 찾을 수 없습니다.");
            }
            return new ItemInfo(auction.getId(), auction.getTitle(), auction.getCurrentPrice());
        }

        Post post = transaction.getPost();
        if (post == null) {
            throw new IllegalStateException("상품 정보를 찾을 수 없습니다.");
        }
        return new ItemInfo(post.getId(), post.getTitle(), post.getPrice());
    }

    // 결제 금액 계산
    private BigDecimal getTransactionAmount(Transaction transaction) {
        if (transaction.getTransactionType() == TransactionType.AUCTION) {
            return transaction.getAuction().getCurrentPrice();
        }
        return transaction.getPost().getPrice();
    }

    // 일반 거래 상품 상태 업데이트
    private void updateItemStatus(Transaction transaction) {
        if (transaction.getTransactionType() == TransactionType.DEFAULT) {
            transaction.getPost().setStatus(SaleStatus.SOLD);
        }
        // 경매는 자동으로 SOLD 처리되므로 별도 로직 불필요
    }

    private void validatePostForSale(Post post) {
        if (post.getStatus() != SaleStatus.ON_SALE) {
            throw new IllegalStateException("판매 중인 상품이 아닙니다.");
        }
    }

    private void validateBuyerNotSeller(User buyer, User seller) {
        if (seller.getId().equals(buyer.getId())) {
            throw new IllegalArgumentException("자신의 상품은 구매할 수 없습니다.");
        }
    }

    private void validatePaymentAmount(Payment payment, long requestAmount) {
        if (payment.getAmount().compareTo(BigDecimal.valueOf(requestAmount)) != 0) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
        }
    }

    private void validateBuyerForReceipt(Transaction transaction, Long buyerId) {
        if (!transaction.getBuyer().getId().equals(buyerId)) {
            throw new IllegalArgumentException("구매자만 상품 수령을 확인할 수 있습니다.");
        }
    }

    private void validateTransactionStatus(Transaction transaction, TransactionStatus expectedStatus) {
        if (transaction.getStatus() != expectedStatus) {
            throw new IllegalArgumentException(
                    String.format("거래 상태가 올바르지 않습니다. 예상: %s, 실제: %s",
                            expectedStatus, transaction.getStatus())
            );
        }
    }

    private User getBuyer(Long buyerId) {
        return userRepository.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("구매자를 찾을 수 없습니다: " + buyerId));
    }

    private boolean isReusableStatus(TransactionStatus status) {
        return status == TransactionStatus.PAYMENT_PENDING || status == TransactionStatus.READY;
    }

    private Pageable defaultSorting(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    private String generateOrderId(TransactionType transactionType) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = UUID.randomUUID().toString()
                .substring(0, 8)
                .toUpperCase();

        String prefix = transactionType == TransactionType.AUCTION ? "AUC_" : "ORDER_";
        return prefix + timestamp + "_" + random;
    }

    private record ItemInfo(Long id, String name, BigDecimal amount) {
    }
}
