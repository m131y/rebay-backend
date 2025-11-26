package com.rebay.rebay_backend.Post.entity;


import com.rebay.rebay_backend.auction.entity.Auction;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "hashtags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToMany(mappedBy = "hashtags")
    @Builder.Default
    private Set<Post> posts = new HashSet<>();

    @ManyToMany(mappedBy = "hashtags")
    @Builder.Default
    private Set<Auction> auctions = new HashSet<>();
}
