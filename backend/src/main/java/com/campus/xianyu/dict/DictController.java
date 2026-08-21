package com.campus.xianyu.dict;

import com.campus.xianyu.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DictController {
    private final CategoryRepository categoryRepository;
    private final CampusRepository campusRepository;

    public DictController(CategoryRepository categoryRepository, CampusRepository campusRepository) {
        this.categoryRepository = categoryRepository;
        this.campusRepository = campusRepository;
    }

    @GetMapping("/categories")
    public ApiResponse<List<OptionResponse>> categories() {
        List<OptionResponse> categories = categoryRepository.findByStatusOrderBySortOrderAscIdAsc("ENABLED")
                .stream()
                .map(category -> new OptionResponse(category.getId(), category.getName()))
                .toList();
        return ApiResponse.ok(categories);
    }

    @GetMapping("/campuses")
    public ApiResponse<List<OptionResponse>> campuses() {
        List<OptionResponse> campuses = campusRepository.findByStatusOrderBySortOrderAscIdAsc("ENABLED")
                .stream()
                .map(campus -> new OptionResponse(campus.getId(), campus.getName()))
                .toList();
        return ApiResponse.ok(campuses);
    }
}