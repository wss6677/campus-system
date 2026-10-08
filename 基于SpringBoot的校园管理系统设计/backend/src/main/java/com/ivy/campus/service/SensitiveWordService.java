package com.ivy.campus.service;

import com.ivy.campus.common.PageResult;
import com.ivy.campus.entity.SensitiveWord;

import java.util.List;

public interface SensitiveWordService {

    PageResult<SensitiveWord> page(Integer pageNum, Integer pageSize, String keyword, String level);

    SensitiveWord getById(Long id);

    Long save(SensitiveWord entity);

    void update(Long id, SensitiveWord entity);

    void remove(Long id);

    List<SensitiveWord> listAll();

    // 命中检测：返回命中词，未命中返回null
    String hit(String content);

    // 命中级别：BLOCK/WARN，未命中返回null
    String hitLevel(String content);
}
