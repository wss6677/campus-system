package com.ivy.campus.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ivy.campus.common.BizException;
import com.ivy.campus.common.PageResult;
import com.ivy.campus.dto.CategoryDTO;
import com.ivy.campus.entity.Announcement;
import com.ivy.campus.entity.Category;
import com.ivy.campus.mapper.AnnouncementMapper;
import com.ivy.campus.mapper.CategoryMapper;
import com.ivy.campus.service.CategoryService;
import com.ivy.campus.util.PageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final AnnouncementMapper announcementMapper;

    @Override
    public PageResult<Category> page(Integer pageNum, Integer pageSize, String keyword, Integer status) {
        IPage<Category> page = categoryMapper.selectPage(PageUtils.of(pageNum, pageSize),
                Wrappers.<Category>lambdaQuery()
                        .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                                .like(Category::getName, keyword)
                                .or().like(Category::getCode, keyword))
                        .eq(status != null, Category::getStatus, status)
                        .orderByAsc(Category::getSort)
                        .orderByAsc(Category::getId));
        return PageResult.of(page);
    }

    @Override
    public Category getById(Long id) {
        return requireCategory(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(CategoryDTO dto) {
        Long count = categoryMapper.selectCount(Wrappers.<Category>lambdaQuery()
                .eq(Category::getName, dto.getName()));
        if (count != null && count > 0) {
            throw new BizException("分类名称已存在");
        }
        Category category = new Category();
        BeanUtils.copyProperties(dto, category);
        category.setId(null);
        category.setNeedAudit(dto.getNeedAudit() == null ? 1 : dto.getNeedAudit());
        category.setSort(dto.getSort() == null ? 0 : dto.getSort());
        category.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.insert(category);
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CategoryDTO dto) {
        requireCategory(id);
        Category category = new Category();
        BeanUtils.copyProperties(dto, category);
        category.setId(id);
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        requireCategory(id);
        Long used = announcementMapper.selectCount(Wrappers.<Announcement>lambdaQuery()
                .eq(Announcement::getCategoryId, id));
        if (used != null && used > 0) {
            throw new BizException("该分类下存在公告，不允许删除");
        }
        categoryMapper.deleteById(id);
    }

    @Override
    public List<Category> listAll() {
        return categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
    }

    private Category requireCategory(Long id) {
        if (id == null) {
            throw new BizException("分类ID不能为空");
        }
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException("分类不存在");
        }
        return category;
    }
}
