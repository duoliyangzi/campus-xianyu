package com.campus.xianyu.dict;

import com.campus.xianyu.product.ProductRepository;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @PostConstruct
    public void init() {
        recompactSortOrders();
    }

    public List<Category> listEnabled() {
        return categoryRepository.findByStatusOrderBySortOrderAscIdAsc("ENABLED");
    }

    @Transactional
    public List<Category> listForAdmin() {
        categoryRepository.deleteByStatus("DISABLED");
        recompactSortOrders();
        return listEnabled();
    }

    @Transactional
    public Category create(String name) {
        Category category = new Category();
        category.setParentId(0L);
        category.setName(name.trim());
        category.setSortOrder(nextSortOrder());
        category.setStatus("ENABLED");
        Category saved = categoryRepository.save(category);
        recompactSortOrders();
        return categoryRepository.findById(saved.getId()).orElse(saved);
    }

    @Transactional
    public Category update(Long id, String name) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在"));
        if (!"ENABLED".equals(category.getStatus())) {
            throw new IllegalArgumentException("分类不存在");
        }
        category.setName(name.trim());
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在"));
        if (!"ENABLED".equals(category.getStatus())) {
            return;
        }
        if (categoryRepository.existsByParentId(id)) {
            throw new IllegalArgumentException("该分类下还有子分类，请先删除子分类");
        }
        if (productRepository.existsByCategoryId(id)) {
            throw new IllegalArgumentException("该分类下还有商品，无法删除");
        }
        categoryRepository.delete(category);
        recompactSortOrders();
    }

    public int nextSortOrder() {
        return categoryRepository.findMaxSortOrderByStatus("ENABLED") + 1;
    }

    @Transactional
    public void recompactSortOrders() {
        List<Category> categories = categoryRepository.findByStatusOrderBySortOrderAscIdAsc("ENABLED");
        for (int index = 0; index < categories.size(); index++) {
            Category category = categories.get(index);
            int expected = index + 1;
            if (!Integer.valueOf(expected).equals(category.getSortOrder())) {
                category.setSortOrder(expected);
            }
        }
        categoryRepository.saveAll(categories);
    }
}
