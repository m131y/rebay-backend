package com.rebay.rebay_backend.Post.initializer;

import com.rebay.rebay_backend.Post.entity.Category;
import com.rebay.rebay_backend.Post.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CategoryInitializer {

    @Bean
    public CommandLineRunner initCategories(CategoryRepository categoryRepository) {
        return args -> {
            // 이미 데이터가 존재하는지 확인하여 중복 삽입 방지
            if (categoryRepository.count() > 0) {
                System.out.println("category already exist.. ");
                return;
            }

            System.out.println("category initializing...");

            List<Category> allCategories = new ArrayList<>();

            // 1. 최상위 카테고리 (Level 1) 생성 및 저장
            Category digitalDevices = saveCategory(categoryRepository, 200, "전자기기", null, allCategories);
            Category homeAppliances = saveCategory(categoryRepository, 300, "생활가전", null, allCategories);
            Category furniture = saveCategory(categoryRepository, 400, "가구/인테리어", null, allCategories);
            Category homeKitchen = saveCategory(categoryRepository, 500, "생활/주방", null, allCategories);
            Category books = saveCategory(categoryRepository, 600, "도서", null, allCategories);
            Category plants = saveCategory(categoryRepository, 700, "식물/반려동물", null, allCategories);
            Category clothes = saveCategory(categoryRepository, 800, "의류/잡화", null, allCategories);
            Category otherUsedItems = saveCategory(categoryRepository, 900, "기타 중고 물품", null, allCategories);

            // 2. 하위 카테고리 (Level 2) 생성 및 저장
            // 200: 전자기기 하위
            Category camera = saveCategory(categoryRepository, 210, "카메라", digitalDevices, allCategories);
            saveCategory(categoryRepository, 240, "노트북/PC", digitalDevices, allCategories);
            Category mobilePhone = saveCategory(categoryRepository, 260, "핸드폰", digitalDevices, allCategories);


            // 300: 생활가전 하위
            saveCategory(categoryRepository, 310, "대형가전", homeAppliances, allCategories);
            saveCategory(categoryRepository, 320, "주방가전", homeAppliances, allCategories);

            // 400: 가구/인테리어 하위
            Category bed = saveCategory(categoryRepository, 410, "침대/매트리스", furniture, allCategories);
            saveCategory(categoryRepository, 420, "소파/테이블", furniture, allCategories);

            // 500: 생활/주방 하위
            Category cooking = saveCategory(categoryRepository, 510, "조리도구", homeKitchen, allCategories);
            saveCategory(categoryRepository, 520, "식기/컵", homeKitchen, allCategories);

            // 800: 의류/잡화 하위
            Category accessories = saveCategory(categoryRepository, 830, "가방/잡화", clothes, allCategories);


            // 3. 세부 카테고리 (Level 3) 생성 및 저장

            // 210: 카메라 하위

            // 260: 핸드폰 하위
            saveCategory(categoryRepository, 261, "아이폰13", mobilePhone, allCategories);
            saveCategory(categoryRepository, 262, "아이폰13 mini", mobilePhone, allCategories);
            saveCategory(categoryRepository, 263, "아이폰13 Pro", mobilePhone, allCategories);
            saveCategory(categoryRepository, 264, "아이폰13 Pro Max", mobilePhone, allCategories);
            saveCategory(categoryRepository, 265, "아이폰14", mobilePhone, allCategories);
            saveCategory(categoryRepository, 266, "아이폰14 Pro", mobilePhone, allCategories);
            saveCategory(categoryRepository, 267, "아이폰14 Pro Max", mobilePhone, allCategories);
            saveCategory(categoryRepository, 268, "아이폰14 Plus", mobilePhone, allCategories);
            saveCategory(categoryRepository, 269, "아이폰15", mobilePhone, allCategories);
            saveCategory(categoryRepository, 270, "아이폰15 Pro", mobilePhone, allCategories);
            saveCategory(categoryRepository, 271, "아이폰15 Pro Max", mobilePhone, allCategories);
            saveCategory(categoryRepository, 272, "아이폰15 Plus", mobilePhone, allCategories);
            saveCategory(categoryRepository, 273, "아이폰16", mobilePhone, allCategories);
            saveCategory(categoryRepository, 274, "아이폰16 Pro", mobilePhone, allCategories);
            saveCategory(categoryRepository, 275, "아이폰16 Pro Max", mobilePhone, allCategories);
            saveCategory(categoryRepository, 276, "아이폰16 Plus", mobilePhone, allCategories);
            saveCategory(categoryRepository, 277, "아이폰17", mobilePhone, allCategories);
            saveCategory(categoryRepository, 278, "아이폰17 Air", mobilePhone, allCategories);
            saveCategory(categoryRepository, 279, "아이폰17 Pro Max", mobilePhone, allCategories);

            // 410: 침대 하위
            saveCategory(categoryRepository, 411, "싱글침대", bed, allCategories);

            // 830: 잡화 하위
            saveCategory(categoryRepository, 831, "명품 가방", accessories, allCategories);

            System.out.println("initializing end " + allCategories.size() + "개 저장됨.");
        };
    }

    @Transactional
    private Category saveCategory(CategoryRepository repository, int code, String name, Category parent, List<Category> list) {
        Category category = Category.builder()
                .code(code)
                .name(name)
                .parent(parent)
                .build();

        Category savedCategory = repository.save(category);
        list.add(savedCategory);
        return savedCategory;
    }
}
