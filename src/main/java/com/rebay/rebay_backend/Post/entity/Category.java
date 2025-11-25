package com.rebay.rebay_backend.Post.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "category", indexes = {
        @Index(name = "idx_category_parent", columnList = "parent_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id; // 내부 관리용 Primary Key

    @Column(unique = true, nullable = false)
    @EqualsAndHashCode.Include
    private int code; // 200, 210, 211 등 분류 코드

    private String name; // 카테고리 이름 (예: 아이폰13)

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @EqualsAndHashCode.Exclude // 지연 로딩 필드 제외
    private Category parent;

    @JsonIgnore
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude // 지연 로딩 필드 제외
    private Set<Category> children;

}