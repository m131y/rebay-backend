package com.rebay.rebay_backend.user.entity;

import com.rebay.rebay_backend.Post.entity.Post;
import com.rebay.rebay_backend.auction.entity.Auction;
import com.rebay.rebay_backend.review.entity.Review;
import com.rebay.rebay_backend.search.entity.Search;
import com.rebay.rebay_backend.social.entity.Like;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name")
    private String fullName;

    private String bio;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    private String providerId;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal totalPoints = BigDecimal.ZERO; // 판매자에게 정산된 총 적립금

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private boolean enabled;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Post> posts = new HashSet<>();

    @OneToMany(mappedBy = "seller", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Auction> auctions = new HashSet<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Like> likes = new HashSet<>();

    @OneToMany(mappedBy = "reviewer", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Review> reviews = new HashSet<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Search> searches = new HashSet<>();

    @PrePersist
    protected void onCreate() { enabled = true; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public void addPoints(BigDecimal amount) {
        this.totalPoints = this.totalPoints.add(amount);
    }
}