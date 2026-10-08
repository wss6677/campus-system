package com.ivy.campus.service;

import com.ivy.campus.common.PageResult;
import com.ivy.campus.dto.TemplateDTO;
import com.ivy.campus.entity.Template;

import java.util.List;

public interface TemplateService {

    PageResult<Template> page(Integer pageNum, Integer pageSize, String keyword, Long categoryId);

    Template getById(Long id);

    Long save(TemplateDTO dto);

    void update(Long id, TemplateDTO dto);

    void remove(Long id);

    List<Template> listAll();
}
