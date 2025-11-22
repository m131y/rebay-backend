package com.rebay.rebay_backend.Post.service;

import com.rebay.rebay_backend.Post.dto.PostRequest;
import com.rebay.rebay_backend.Post.dto.PostResponse;
import com.rebay.rebay_backend.Post.entity.Category;
import com.rebay.rebay_backend.Post.entity.Hashtag;
import com.rebay.rebay_backend.Post.entity.Post;
import com.rebay.rebay_backend.Post.entity.SaleStatus;
import com.rebay.rebay_backend.Post.exception.UnauthorizedException;
import com.rebay.rebay_backend.Post.repository.CategoryRepository;
import com.rebay.rebay_backend.Post.repository.HashTagRepository;
import com.rebay.rebay_backend.Post.repository.PostRepository;
import com.rebay.rebay_backend.social.repository.LikeRepository;
import com.rebay.rebay_backend.user.dto.UserResponse;
import com.rebay.rebay_backend.user.entity.User;
import com.rebay.rebay_backend.user.exception.ResourceNotFoundException;
import com.rebay.rebay_backend.user.service.AuthenticationService;
import com.rebay.rebay_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final CategoryRepository categoryRepository;
    private final LikeRepository likeRepository;
    private final HashTagRepository hashTagRepository;

    public PostResponse createPost(PostRequest request) {

        User currentUser = authenticationService.getCurrentUser();

        Category currentCategory = categoryRepository.findByCode(request.getCategoryCode())
                .orElseThrow(() -> new ResourceNotFoundException("카테고리를 찾을 수 없습니다."));

        List<String> images = sanitizeImages(request.getImageUrls());

        //대표사진
        String cover = (request.getImageUrl() != null && !request.getImageUrl().isBlank())
                ? request.getImageUrl()
                : (!images.isEmpty() ? images.get(0) : null);


        Post post = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .price(request.getPrice())
                .category(currentCategory)
                .status(SaleStatus.ON_SALE)
                .user(currentUser)
                .imageUrl(cover)
                .imageUrls(images)
                .build();

        if (request.getHashtags() != null && !request.getHashtags().isEmpty()) {
            for (String hashName : request.getHashtags()) {
                Hashtag hashtag = hashTagRepository.findByName(hashName)
                        .orElseGet(() -> hashTagRepository.save(
                                Hashtag.builder()
                                        .name(hashName)
                                        .build()
                        ));
                post.addHashtag(hashtag);

            }
        }

        Post savedPost = postRepository.save(post);
        UserResponse userResponse = userService.mapToUserResponse(currentUser);

        return PostResponse.from(savedPost, userResponse);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getPosts(Pageable pageable) {
        User currentUser = authenticationService.getCurrentUser();

        Page<Post> posts = postRepository.findAllWithUser(pageable);
        return posts.map(post -> {
            UserResponse userResponse = userService.mapToUserResponse(post.getUser());
            PostResponse response = PostResponse.from(post,userResponse);
            Long likeCount = likeRepository.countByPostId(post.getId());
            boolean isLiked = likeRepository.existsByUserAndPost(currentUser, post);

            response.setLiked(isLiked);
            response.setLikeCount(likeCount);

            return response;
        });
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getUserPost(Long userId, Pageable pageable) {
        User currentUser = authenticationService.getCurrentUser();
        Page<Post> posts = postRepository.findByUserId(userId, pageable);
        return posts.map(post -> {
            UserResponse userResponse = userService.mapToUserResponse(post.getUser());
            PostResponse response = PostResponse.from(post, userResponse);
            Long likeCount = likeRepository.countByPostId(post.getId());
            boolean isLiked = likeRepository.existsByUserAndPost(currentUser, post);

            response.setLiked(isLiked);
            response.setLikeCount(likeCount);
            return response;
        });
    }


    //조회수 증가하면서 게시글 상세 조회
    @Transactional
    public PostResponse updateViewcount(Long postId) {
        postRepository.updateView(postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        UserResponse userResponse = userService.mapToUserResponse(post.getUser());

        return PostResponse.from(post, userResponse);
    }


    public PostResponse updatePost(Long postId, PostRequest request) {

        User currentUser = authenticationService.getCurrentUser();

        Category currentCategory = categoryRepository.findByCode(request.getCategoryCode())
                .orElseThrow(() -> new ResourceNotFoundException("카테고리를 찾을 수 없습니다."));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to update this post");
        }

        List<String> images = sanitizeImages(request.getImageUrls());
        String cover = (request.getImageUrl() != null && !request.getImageUrl().isBlank())
                ? request.getImageUrl()
                : (!images.isEmpty() ? images.get(0) : null);

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setPrice(request.getPrice());
        post.setCategory(currentCategory);
        post.setStatus(request.getStatus() == null ? SaleStatus.ON_SALE: request.getStatus() );
        post.setImageUrl(cover);
        post.setImageUrls(images);

        post.getHashtags().clear();

        if (request.getHashtags() != null && !request.getHashtags().isEmpty()) {
            for (String hashName : request.getHashtags()) {
                Hashtag hashtag = hashTagRepository.findByName(hashName)
                        .orElseGet(() -> hashTagRepository.save(
                                Hashtag.builder()
                                        .name(hashName)
                                        .build()
                        ));
                post.addHashtag(hashtag);

            }
        }

        Post updatedPost = postRepository.save(post);
        UserResponse userResponse = userService.mapToUserResponse(currentUser);

        return PostResponse.from(updatedPost, userResponse);
    }

    public void deletePost(Long postId) {

        User currentUser = authenticationService.getCurrentUser();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to update this post");
        }

        postRepository.delete(post);
    }

    public Long getPostsCountByUser(Long userId) {
        return postRepository.countByUserId(userId);
    }

    public List<String> sanitizeImages(List<String> src) {
        if (src == null) return new ArrayList<>();
        List<String> out = new ArrayList<>();
        for (String s : src) {
            if (s != null) {
                String t = s.trim();
                if (!t.isEmpty()) out.add(t);
            }
        }
        return out;
    }



}