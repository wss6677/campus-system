package com.ivy.campus.service;

import com.ivy.campus.common.PageResult;
import com.ivy.campus.dto.CategoryDTO;
import com.ivy.campus.entity.Category;

import java.util.List;

public interface CategoryService {

    PageResult<Category> page(Integer pageNum, Integer pageSize, String keyword, Integer status);

    Category getById(Long id);

    Long save(CategoryDTO dto);

    void update(Long id, CategoryDTO dto);

    void remove(Long id);

    List<Category> listAll();
}
