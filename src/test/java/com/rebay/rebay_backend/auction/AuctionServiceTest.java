package com.rebay.rebay_backend.auction;

import com.rebay.rebay_backend.Post.entity.Category;
import com.rebay.rebay_backend.Post.entity.SaleStatus;
import com.rebay.rebay_backend.Post.repository.CategoryRepository;
import com.rebay.rebay_backend.auction.entity.Auction;
import com.rebay.rebay_backend.auction.repository.AuctionRepository;
import com.rebay.rebay_backend.auction.repository.BidHistoryRepository;
import com.rebay.rebay_backend.auction.service.AuctionService;
import com.rebay.rebay_backend.user.entity.User;
import com.rebay.rebay_backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "openai.keys.default=openai-key",
        "AWS_ACCESS_KEY_ID=access-key",
        "AWS_SECRET_ACCESS_KEY=secret-key",
        "AWS_REGION=us-east-1",
        "AWS_BUCKET_NAME=bucket",
        "jwt.secret=VGhpcyBpcyBhIGR1bW15IHNlY3JldCBrZXkgZm9yIHRlc3Rpbmc=",
        "jwt.expiration=3600000",
        "jwt.refresh-expiration=86400000",
        "frontend.url=http://localhost:3000"
})
public class AuctionServiceTest {

    @Autowired private AuctionService auctionService;
    @Autowired private AuctionRepository auctionRepository;
    @Autowired private BidHistoryRepository bidHistoryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;

    @Test
    @DisplayName("동시에 100명이 입찰을 시도하면 순차적으로 처리되어야 한다")
    void bidConcurrencyTest() throws InterruptedException {
        // Given: 테스트용 데이터 준비

        User seller = userRepository.save(User.builder()
                .username("seller_test_" + System.currentTimeMillis())
                .email("seller_" + System.currentTimeMillis() + "@test.com")
                .password("pass")
                .fullName("Seller")
                .build());

        User bidder = userRepository.save(User.builder()
                .username("bidder_test_" + System.currentTimeMillis())
                .email("bidder_" + System.currentTimeMillis() + "@test.com")
                .password("pass")
                .fullName("Bidder")
                .build());

        Category category = categoryRepository.save(Category.builder()
                .code(9999)
                .name("테스트 카테고리")
                .build());

        Auction auction = auctionRepository.save(Auction.builder()
                .seller(seller)
                .category(category)
                .title("테스트 경매품")
                .content("테스트입니다")
                .price(BigDecimal.valueOf(1000))
                .currentPrice(BigDecimal.valueOf(1000))
                .startTime(LocalDateTime.now().minusMinutes(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .status(SaleStatus.ON_SALE)
                .build());

        Long auctionId = auction.getId();
        Long bidderId = bidder.getId();

        // When: 동시 입찰 시도
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0); // 성공 횟수 카운터

        for (int i = 0; i < threadCount; i++) {
            final BigDecimal bidAmount = BigDecimal.valueOf(2000 + i * 100);

            executorService.submit(() -> {
                try {
                    auctionService.placeBid(auctionId, bidAmount, bidderId);

                    // 예외 없이 성공했을 때만 카운트 증가
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.out.println("입찰 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // Then: 검증
        long count = bidHistoryRepository.count();
        long dbCount = bidHistoryRepository.count();
        Auction updatedAuction = auctionRepository.findById(auctionId).orElseThrow();


        System.out.println("------------");
        System.out.println("테스트 종료");
        System.out.println("DB에 저장된 총 입찰 수: " + count);
        System.out.println("최종 낙찰가: " + updatedAuction.getCurrentPrice());
        System.out.println("------------");

        assertThat(updatedAuction.getCurrentPrice()).isGreaterThan(BigDecimal.valueOf(1000));
        assertThat(dbCount).isEqualTo(successCount.get()); // DB에 쌓인 데이터수 == 성공횟수 카운트

    }
}