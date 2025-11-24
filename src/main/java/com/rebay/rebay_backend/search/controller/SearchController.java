package com.rebay.rebay_backend.search.controller;

import com.rebay.rebay_backend.Post.dto.PostResponse;
import com.rebay.rebay_backend.search.entity.SearchTarget;
import com.rebay.rebay_backend.search.service.SearchService;
import com.rebay.rebay_backend.user.dto.UserResponse;
import com.rebay.rebay_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;
    private final UserService userService;


    @GetMapping("/posts")
    public Page<PostResponse> searchPosts(
            @RequestParam String keyword,
            @RequestParam SearchTarget target,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return searchService.searchPost(keyword, target, pageable)
                .map(post -> { UserResponse userResponse = userService.mapToUserResponse(post.getUser());
                    return PostResponse.fromEntity(post, userResponse);});
    }

    @GetMapping("/history")
    public ResponseEntity<Set<String>> getSearchHistory() {
        return ResponseEntity.ok(searchService.getSearchHistory());
    }

    @GetMapping("/suggest")
    public Page<String> suggest(
            @RequestParam String keyword,
            @RequestParam SearchTarget target,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return searchService.suggest(keyword, target, pageable);
    }
}
