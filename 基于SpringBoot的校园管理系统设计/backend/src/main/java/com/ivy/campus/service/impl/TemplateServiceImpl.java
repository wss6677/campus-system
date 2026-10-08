package com.ivy.campus.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ivy.campus.common.BizException;
import com.ivy.campus.common.PageResult;
import com.ivy.campus.dto.TemplateDTO;
import com.ivy.campus.entity.Template;
import com.ivy.campus.mapper.TemplateMapper;
import com.ivy.campus.security.LoginUser;
import com.ivy.campus.security.UserContext;
import com.ivy.campus.service.TemplateService;
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
public class TemplateServiceImpl implements TemplateService {

    private final TemplateMapper templateMapper;

    @Override
    public PageResult<Template> page(Integer pageNum, Integer pageSize, String keyword, Long categoryId) {
        IPage<Template> page = templateMapper.selectPage(PageUtils.of(pageNum, pageSize),
                Wrappers.<Template>lambdaQuery()
                        .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                                .like(Template::getName, keyword)
                                .or().like(Template::getTitle, keyword))
                        .eq(categoryId != null, Template::getCategoryId, categoryId)
                        .orderByDesc(Template::getId));
        return PageResult.of(page);
    }

    @Override
    public Template getById(Long id) {
        return requireTemplate(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(TemplateDTO dto) {
        Template template = new Template();
        BeanUtils.copyProperties(dto, template);
        template.setId(null);
        template.setUseCount(0);
        LoginUser current = UserContext.get();
        template.setCreatorId(current == null ? null : current.getUserId());
        template.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        template.setCreateTime(LocalDateTime.now());
        templateMapper.insert(template);
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TemplateDTO dto) {
        requireTemplate(id);
        Template template = new Template();
        BeanUtils.copyProperties(dto, template);
        template.setId(id);
        templateMapper.updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        requireTemplate(id);
        templateMapper.deleteById(id);
    }

    @Override
    public List<Template> listAll() {
        return templateMapper.selectList(Wrappers.<Template>lambdaQuery()
                .eq(Template::getStatus, 1)
                .orderByDesc(Template::getId));
    }

    private Template requireTemplate(Long id) {
        if (id == null) {
            throw new BizException("模板ID不能为空");
        }
        Template template = templateMapper.selectById(id);
        if (template == null) {
            throw new BizException("模板不存在");
        }
        return template;
    }
}
