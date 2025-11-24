package com.rebay.rebay_backend.integratedProduct.repository;

import com.rebay.rebay_backend.Post.dto.PostResponse;
import com.rebay.rebay_backend.Post.entity.Post;
import com.rebay.rebay_backend.Post.entity.SaleStatus; // SaleStatus import 추가
import com.rebay.rebay_backend.Post.repository.PostRepository;
import com.rebay.rebay_backend.Post.service.PostService;
import com.rebay.rebay_backend.auction.dto.AuctionResponse;
import com.rebay.rebay_backend.auction.entity.Auction;
import com.rebay.rebay_backend.auction.repository.AuctionRepository;
import com.rebay.rebay_backend.auction.service.AuctionService;
import com.rebay.rebay_backend.integratedProduct.dto.IntegratedProductResponse;
import com.rebay.rebay_backend.integratedProduct.dto.ProductFeedItem;
import com.rebay.rebay_backend.integratedProduct.dto.ProductType; // ProductType import 추가
import com.rebay.rebay_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal; // BigDecimal import 추가
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class IntegratedProductRepository {
    private final PostRepository postRepository;
    private final AuctionRepository auctionRepository;
    private final UserService userService;
    private final PostService postService;
    private final AuctionService auctionService;

    public Page<ProductFeedItem> findIntegratedProducts(
            int page,
            int size,
            Integer categoryCode,
            String sort,
            Boolean excludeSold,
            ProductType productType
    ) {
        Pageable pageable = PageRequest.of(page, size);

        List<Post> postData = postRepository.findAllWithUser();
        List<Auction> auctionData = auctionRepository.findAllWithUser();

        List<PostResponse> posts = postData.stream()
                .map(post -> PostResponse.fromEntity(post, userService.mapToUserResponse(post.getUser())))
                .toList();

        List<AuctionResponse> auctions = auctionData.stream()
                .map(auction -> AuctionResponse.fromEntity(auction, userService.mapToUserResponse(auction.getSeller())))
                .toList();

        List<IntegratedProductResponse> allIntegratedProducts = new ArrayList<>();
        posts.stream().map(IntegratedProductResponse::from).forEach(allIntegratedProducts::add);
        auctions.stream().map(IntegratedProductResponse::from).forEach(allIntegratedProducts::add);


        List<IntegratedProductResponse> filteredProducts = allIntegratedProducts.stream()
                // 상품 유형 필터링 (ProductType)
                .filter(product -> {
                    if (productType == null ) {
                        return true;
                    } else {
                        return product.getProductType() == productType;
                    }
                })
                // 카테고리 필터링
                .filter(product -> {
                    if (categoryCode == null) {
                        return true;
                    }
                    int productCode = product.getProductData().getCategoryCode();
                    int filterCode = categoryCode.intValue();

                    if (filterCode % 100 == 0) {
                        return (productCode >= filterCode) && (productCode <= filterCode + 99);
                    }
                    else if (filterCode % 10 == 0) {
                        return (productCode >= filterCode) && (productCode <= filterCode + 9);
                    }
                    else {
                        return productCode == filterCode;
                    }
                })
                // 판매 완료 제외 필터링
                .filter(product -> {
                    if (excludeSold != null && excludeSold) {
                        return product.getProductData().getStatus() != SaleStatus.SOLD;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        Comparator<IntegratedProductResponse> comparator;

        if (sort != null) {
            switch (sort.toUpperCase()) {
                case "PRICE_ASC":
                    comparator = Comparator.comparing(product -> product.getProductData().getPrice());
                    break;
                case "PRICE_DESC":
                    comparator = Comparator.comparing(product -> product.getProductData().getPrice(), Comparator.reverseOrder());
                    break;
                case "TITLE_ASC":
                    comparator = Comparator.comparing(product -> product.getProductData().getTitle());
                    break;
                case "LATEST":
                default:
                    comparator = Comparator.comparing(
                            product -> product.getProductData().getCreatedAt(),
                            Comparator.reverseOrder()
                    );
                    break;
            }
        } else {
            comparator = Comparator.comparing(
                    product -> product.getProductData().getCreatedAt(),
                    Comparator.reverseOrder()
            );
        }

        filteredProducts.sort(comparator);

        int totalElements = filteredProducts.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), totalElements);

        List<IntegratedProductResponse> pageContent;
        if (start >= totalElements) {
            pageContent = List.of();
        } else {
            pageContent = filteredProducts.subList(start, end);
        }

        List<ProductFeedItem> feedItems = pageContent.stream()
                .map(IntegratedProductResponse::toProductFeedItem)
                .collect(Collectors.toList());

        return new PageImpl<>(
                feedItems,
                pageable,
                totalElements
        );
    }

    public List<ProductFeedItem> findUserIntegratedProducts(Long userId) {
        List<PostResponse> postData = postService.getUserPost(userId);
        List<AuctionResponse> auctionData = auctionService.getUserAuctions(userId);

        List<IntegratedProductResponse> allIntegratedProducts = new ArrayList<>();
        postData.stream().map(IntegratedProductResponse::from).forEach(allIntegratedProducts::add);
        auctionData.stream().map(IntegratedProductResponse::from).forEach(allIntegratedProducts::add);

        return fromIntegratedProductToFeedItem(allIntegratedProducts);
    }

    public List<IntegratedProductResponse> fromDataToIntegratedProduct(List<Post> postData, List<Auction> auctionData) {
        List<PostResponse> posts = postData.stream()
                .map(post -> PostResponse.fromEntity(post, userService.mapToUserResponse(post.getUser())))
                .toList();

        List<AuctionResponse> auctions = auctionData.stream()
                .map(auction -> AuctionResponse.fromEntity(auction, userService.mapToUserResponse(auction.getSeller())))
                .toList();

        List<IntegratedProductResponse> allIntegratedProducts = new ArrayList<>();
        posts.stream().map(IntegratedProductResponse::from).forEach(allIntegratedProducts::add);
        auctions.stream().map(IntegratedProductResponse::from).forEach(allIntegratedProducts::add);

        return allIntegratedProducts;
    }

    public List<ProductFeedItem> fromIntegratedProductToFeedItem(List<IntegratedProductResponse> allIntegratedProducts) {
        List<ProductFeedItem> userProducts = allIntegratedProducts.stream()
                .sorted(Comparator.comparing(IntegratedProductResponse::getCreatedAt).reversed())
                .map(IntegratedProductResponse::toProductFeedItem)
                .collect(Collectors.toList());

        return userProducts;
    }
}