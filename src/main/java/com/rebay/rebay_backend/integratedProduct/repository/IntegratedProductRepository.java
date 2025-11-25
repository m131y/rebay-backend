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