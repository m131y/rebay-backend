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

            // 최상위 카테고리 (Level 1) 생성 및 저장
            Category digitalDevices = saveCategory(categoryRepository, 200, "전자기기", null, allCategories);
            Category homeAppliances = saveCategory(categoryRepository, 300, "생활가전", null, allCategories);
            Category furniture = saveCategory(categoryRepository, 400, "가구/인테리어", null, allCategories);
            Category homeKitchen = saveCategory(categoryRepository, 500, "생활/주방", null, allCategories);
            Category books = saveCategory(categoryRepository, 600, "도서", null, allCategories);
            Category plants = saveCategory(categoryRepository, 700, "식물/반려동물", null, allCategories);
            Category clothes = saveCategory(categoryRepository, 800, "의류/잡화", null, allCategories);
            Category otherUsedItems = saveCategory(categoryRepository, 900, "기타 중고 물품", null, allCategories);

            // 200: 전자기기 하위
            Category camera = saveCategory(categoryRepository, 210, "카메라", digitalDevices, allCategories);
            Category audio = saveCategory(categoryRepository, 220, "음향기기", digitalDevices, allCategories); // 추가
            Category gameConsole = saveCategory(categoryRepository, 230, "게임/타이틀", digitalDevices, allCategories); // 추가
            Category notebookPC = saveCategory(categoryRepository, 240, "노트북/PC", digitalDevices, allCategories); // 변수명 변경
            Category monitor = saveCategory(categoryRepository, 250, "모니터/주변기기", digitalDevices, allCategories); // 추가
            Category mobilePhone = saveCategory(categoryRepository, 260, "핸드폰", digitalDevices, allCategories);
            Category wearable = saveCategory(categoryRepository, 280, "스마트워치/밴드", digitalDevices, allCategories); // 추가

            // 300: 생활가전 하위
            Category largeAppliance = saveCategory(categoryRepository, 310, "대형가전", homeAppliances, allCategories);
            Category kitchenAppliance = saveCategory(categoryRepository, 320, "주방가전", homeAppliances, allCategories);
            Category airAppliance = saveCategory(categoryRepository, 330, "계절가전/공기", homeAppliances, allCategories); // 추가
            Category beautyHealth = saveCategory(categoryRepository, 340, "미용/건강가전", homeAppliances, allCategories); // 추가

            // 400: 가구/인테리어 하위
            Category bed = saveCategory(categoryRepository, 410, "침대/매트리스", furniture, allCategories);
            Category sofaTable = saveCategory(categoryRepository, 420, "소파/테이블", furniture, allCategories);
            Category storage = saveCategory(categoryRepository, 430, "수납/서랍장", furniture, allCategories); // 추가
            Category lighting = saveCategory(categoryRepository, 440, "조명/DIY", furniture, allCategories); // 추가

            // 500: 생활/주방 하위
            Category cooking = saveCategory(categoryRepository, 510, "조리도구", homeKitchen, allCategories);
            Category tableware = saveCategory(categoryRepository, 520, "식기/컵", homeKitchen, allCategories);
            Category fabric = saveCategory(categoryRepository, 530, "침구/패브릭", homeKitchen, allCategories); // 추가
            Category cleaning = saveCategory(categoryRepository, 540, "청소/세탁용품", homeKitchen, allCategories); // 추가

            // 600: 도서 하위
            Category fiction = saveCategory(categoryRepository, 610, "소설/에세이", books, allCategories); // 추가
            Category education = saveCategory(categoryRepository, 620, "학습/수험서", books, allCategories); // 추가

            // 700: 식물/반려동물 하위
            Category plantItems = saveCategory(categoryRepository, 710, "화분/정원용품", plants, allCategories); // 추가
            Category petItems = saveCategory(categoryRepository, 720, "반려동물용품", plants, allCategories); // 추가

            // 800: 의류/잡화 하위
            Category top = saveCategory(categoryRepository, 810, "상의/아우터", clothes, allCategories); // 추가
            Category bottom = saveCategory(categoryRepository, 820, "하의/원피스", clothes, allCategories); // 추가
            Category accessories = saveCategory(categoryRepository, 830, "가방/잡화", clothes, allCategories);
            Category shoes = saveCategory(categoryRepository, 840, "신발", clothes, allCategories); // 추가
            Category jewelry = saveCategory(categoryRepository, 850, "시계/쥬얼리", clothes, allCategories); // 추가

            // --- 200: 전자기기 하위 ---
            // 210: 카메라 하위
            saveCategory(categoryRepository, 211, "DSLR/미러리스", camera, allCategories);
            saveCategory(categoryRepository, 212, "필름/토이카메라", camera, allCategories);
            saveCategory(categoryRepository, 213, "액션캠/드론", camera, allCategories);

            // 220: 음향기기 하위
            saveCategory(categoryRepository, 221, "이어폰/헤드폰", audio, allCategories);
            saveCategory(categoryRepository, 222, "스피커/앰프", audio, allCategories);

            // 230: 게임/타이틀 하위
            saveCategory(categoryRepository, 231, "PlayStation 5", gameConsole, allCategories);
            saveCategory(categoryRepository, 232, "PlayStation 5 pro", gameConsole, allCategories);
            saveCategory(categoryRepository, 233, "닌텐도 스위치", gameConsole, allCategories);
            saveCategory(categoryRepository, 234, "닌텐도 스위치 라이트", gameConsole, allCategories);
            saveCategory(categoryRepository, 235, "닌텐도 스위치 2", gameConsole, allCategories);

            // 240: 노트북/PC 하위
            saveCategory(categoryRepository, 241, "MacBook Air 13", notebookPC, allCategories);
            saveCategory(categoryRepository, 242, "MacBook Air 15", notebookPC, allCategories);
            saveCategory(categoryRepository, 243, "MacBook Pro 14", notebookPC, allCategories);
            saveCategory(categoryRepository, 244, "MacBook Pro 16", notebookPC, allCategories);

            // 250: 모니터/주변기기 하위
            saveCategory(categoryRepository, 251, "모니터", monitor, allCategories);
            saveCategory(categoryRepository, 252, "키보드/마우스", monitor, allCategories);
            saveCategory(categoryRepository, 253, "프린터/스캐너", monitor, allCategories);

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

            // --- 300: 생활가전 하위 ---
            // 310: 대형가전 하위
            saveCategory(categoryRepository, 311, "TV", largeAppliance, allCategories);
            saveCategory(categoryRepository, 312, "냉장고/김치냉장고", largeAppliance, allCategories);
            saveCategory(categoryRepository, 313, "세탁기/건조기", largeAppliance, allCategories);

            // 320: 주방가전 하위
            saveCategory(categoryRepository, 321, "커피머신/포트", kitchenAppliance, allCategories);
            saveCategory(categoryRepository, 322, "전자레인지/오븐", kitchenAppliance, allCategories);

            // 330: 계절가전/공기 하위
            saveCategory(categoryRepository, 331, "에어컨/냉방", airAppliance, allCategories);
            saveCategory(categoryRepository, 332, "난방기/온풍기", airAppliance, allCategories);
            saveCategory(categoryRepository, 333, "공기청정기/가습기", airAppliance, allCategories);

            // --- 400: 가구/인테리어 하위 ---
            // 410: 침대 하위
            saveCategory(categoryRepository, 411, "싱글침대", bed, allCategories);
            saveCategory(categoryRepository, 412, "더블/퀸/킹 침대", bed, allCategories);
            saveCategory(categoryRepository, 413, "화장대/협탁", bed, allCategories); // 추가

            // 420: 소파/테이블 하위
            saveCategory(categoryRepository, 421, "패브릭/가죽 소파", sofaTable, allCategories);
            saveCategory(categoryRepository, 422, "식탁/책상", sofaTable, allCategories);

            // 430: 수납/서랍장 하위
            saveCategory(categoryRepository, 431, "책장/선반", storage, allCategories);
            saveCategory(categoryRepository, 432, "옷장/붙박이장", storage, allCategories);

            // 440: 조명/DIY 하위
            saveCategory(categoryRepository, 441, "스탠드/장스탠드", lighting, allCategories);
            saveCategory(categoryRepository, 442, "인테리어 소품", lighting, allCategories);

            // --- 500: 생활/주방 하위 ---
            // 510: 조리도구 하위
            saveCategory(categoryRepository, 511, "냄비/프라이팬", cooking, allCategories);
            saveCategory(categoryRepository, 512, "칼/도마", cooking, allCategories);

            // 520: 식기/컵 하위
            saveCategory(categoryRepository, 521, "접시/그릇", tableware, allCategories);
            saveCategory(categoryRepository, 522, "머그/와인잔", tableware, allCategories);

            // 530: 침구/패브릭 하위
            saveCategory(categoryRepository, 531, "이불/베개", fabric, allCategories);
            saveCategory(categoryRepository, 532, "커튼/블라인드", fabric, allCategories);

            // --- 800: 의류/잡화 하위 ---
            // 810: 상의/아우터 하위
            saveCategory(categoryRepository, 811, "티셔츠/셔츠", top, allCategories);
            saveCategory(categoryRepository, 812, "맨투맨/후드티", top, allCategories);
            saveCategory(categoryRepository, 813, "코트/자켓", top, allCategories);

            // 820: 하의/원피스 하위
            saveCategory(categoryRepository, 821, "청바지/슬랙스", bottom, allCategories);
            saveCategory(categoryRepository, 822, "스커트/원피스", bottom, allCategories);

            // 830: 가방/잡화 하위
            saveCategory(categoryRepository, 831, "명품 가방", accessories, allCategories);
            saveCategory(categoryRepository, 832, "지갑/벨트", accessories, allCategories);
            saveCategory(categoryRepository, 833, "모자/장갑", accessories, allCategories);

            // 840: 신발 하위
            saveCategory(categoryRepository, 841, "운동화/스니커즈", shoes, allCategories);
            saveCategory(categoryRepository, 842, "구두/부츠", shoes, allCategories);

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
