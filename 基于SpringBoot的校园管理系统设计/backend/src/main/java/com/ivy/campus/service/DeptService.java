package com.ivy.campus.service;

import com.ivy.campus.common.PageResult;
import com.ivy.campus.dto.DeptDTO;
import com.ivy.campus.vo.DeptVO;

import java.util.List;

public interface DeptService {

    PageResult<DeptVO> page(Integer pageNum, Integer pageSize, String keyword, String type, Long parentId);

    DeptVO getById(Long id);

    Long save(DeptDTO dto);

    void update(Long id, DeptDTO dto);

    void remove(Long id);

    List<DeptVO> listAll();

    List<DeptVO> tree();
}
