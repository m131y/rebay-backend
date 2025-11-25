package com.rebay.rebay_backend.search.service;

import com.rebay.rebay_backend.Post.dto.PostResponse;
import com.rebay.rebay_backend.Post.entity.Post;
import com.rebay.rebay_backend.Post.repository.PostRepository;
import com.rebay.rebay_backend.auction.dto.AuctionResponse;
import com.rebay.rebay_backend.auction.entity.Auction;
import com.rebay.rebay_backend.auction.repository.AuctionRepository;
import com.rebay.rebay_backend.integratedProduct.dto.IntegratedProductResponse;
import com.rebay.rebay_backend.integratedProduct.dto.ProductFeedItem;
import com.rebay.rebay_backend.integratedProduct.repository.IntegratedProductRepository;
import com.rebay.rebay_backend.search.entity.Search;
import com.rebay.rebay_backend.search.entity.SearchTarget;
import com.rebay.rebay_backend.search.repository.SearchRepository;
import com.rebay.rebay_backend.user.entity.User;
import com.rebay.rebay_backend.user.service.AuthenticationService;
import com.rebay.rebay_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final PostRepository postRepository;
    private final SearchRepository searchRepository;
    private final AuctionRepository auctionRepository;
    private final IntegratedProductRepository integratedProductRepository;
    private final AuthenticationService authenticationService;
    private final UserService userService;

    public List<ProductFeedItem> searchPost(String keyword, SearchTarget target) {
        User currentUser = authenticationService.getCurrentUser();

        String kw = (keyword == null) ? "" : keyword.trim();
        if (kw.isEmpty()) return new ArrayList<>();
        Search search = Search.builder()
                .user(currentUser)
                .keyword(kw)
                .createdAt(LocalDateTime.now())
                .build();

        searchRepository.save(search);

        switch (target) {
            case TITLE:
                List<Post> postTitleData = postRepository.findByTitleContains(kw);
                List<Auction> auctionTitleData = auctionRepository.findByTitleContains(kw);
                List<IntegratedProductResponse> titleProductList = integratedProductRepository.fromDataToIntegratedProduct(postTitleData, auctionTitleData);
                return integratedProductRepository.fromIntegratedProductToFeedItem(titleProductList);

            case USERNAME:
                List<Post> postUsernameData = postRepository.findByUsernameContains(kw);
                List<Auction> auctionUsernameData = auctionRepository.findByUsernameContains(kw);
                List<IntegratedProductResponse> usernameProductList = integratedProductRepository.fromDataToIntegratedProduct(postUsernameData, auctionUsernameData);
                return integratedProductRepository.fromIntegratedProductToFeedItem(usernameProductList);

            case HASHTAG:
                List<Post> postHashtagData = postRepository.findByHashtagExact(kw);
                List<Auction> auctionHashtagData = auctionRepository.findByHashtagExact(kw);
                List<IntegratedProductResponse> hashtagProductList = integratedProductRepository.fromDataToIntegratedProduct(postHashtagData, auctionHashtagData);
                return integratedProductRepository.fromIntegratedProductToFeedItem(hashtagProductList);

        }
        return new ArrayList<>();
    }

    public Set<String> getSearchHistory() {
        User currentUser = authenticationService.getCurrentUser();
        List<Search> history = searchRepository.findByUserIdOrderByCreatedAt(currentUser.getId());
        Set<String> keywordList = history.stream()
                .map(Search::getKeyword)
                .collect(Collectors.toSet());

        return keywordList;
    }

    public Page<String> suggest(String keyword, SearchTarget target , Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Page.empty(pageable);
        }

        List<String> combinedList;
        long totalElements = 0;

        switch (target) {
            case TITLE:
                Page<String> postTitleData = postRepository.suggestTitle(keyword, pageable);
                Page<String> auctionTitleData = auctionRepository.suggestTitle(keyword, pageable);

                combinedList = Stream.concat(postTitleData.stream(), auctionTitleData.stream())
                        .distinct()
                        // 필요하다면 .sorted() 추가
                        .collect(Collectors.toList());
                totalElements = postTitleData.getTotalElements() + auctionTitleData.getTotalElements();
                break;

            case USERNAME:
                Page<String> postUsernameData = postRepository.suggestUsername(keyword, pageable);
                Page<String> auctionUsernameData = auctionRepository.suggestUsername(keyword, pageable);

                combinedList = Stream.concat(postUsernameData.stream(), auctionUsernameData.stream())
                        .distinct()
                        .collect(Collectors.toList());
                totalElements = postUsernameData.getTotalElements() + auctionUsernameData.getTotalElements();
                break;

            case HASHTAG:
                Page<String> postHashtagData = postRepository.suggestHashtag(keyword, pageable);
                Page<String> auctionHashtagData = auctionRepository.suggestHashtag(keyword, pageable);

                combinedList = Stream.concat(postHashtagData.stream(), auctionHashtagData.stream())
                        .distinct()
                        .collect(Collectors.toList());
                totalElements = postHashtagData.getTotalElements() + auctionHashtagData.getTotalElements();
                break;

            default:
                return Page.empty(pageable);
        }
        return new PageImpl<>(combinedList, pageable, totalElements);
    }
}
