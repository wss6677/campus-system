package com.ivy.campus.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ivy.campus.common.BizException;
import com.ivy.campus.common.PageResult;
import com.ivy.campus.dto.DeptDTO;
import com.ivy.campus.entity.Dept;
import com.ivy.campus.entity.User;
import com.ivy.campus.mapper.DeptMapper;
import com.ivy.campus.mapper.UserMapper;
import com.ivy.campus.service.DeptService;
import com.ivy.campus.util.PageUtils;
import com.ivy.campus.vo.DeptVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeptServiceImpl implements DeptService {

    private final DeptMapper deptMapper;
    private final UserMapper userMapper;

    @Override
    public PageResult<DeptVO> page(Integer pageNum, Integer pageSize, String keyword, String type, Long parentId) {
        IPage<Dept> page = deptMapper.selectPage(PageUtils.of(pageNum, pageSize),
                Wrappers.<Dept>lambdaQuery()
                        .like(keyword != null && !keyword.isBlank(), Dept::getName, keyword)
                        .eq(type != null && !type.isBlank(), Dept::getType, type)
                        .eq(parentId != null, Dept::getParentId, parentId)
                        .orderByAsc(Dept::getSort)
                        .orderByAsc(Dept::getId));
        List<DeptVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public DeptVO getById(Long id) {
        return toVO(requireDept(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(DeptDTO dto) {
        Dept dept = new Dept();
        BeanUtils.copyProperties(dto, dept);
        dept.setId(null);
        dept.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        dept.setType(dto.getType() == null || dto.getType().isBlank() ? "DEPT" : dto.getType());
        dept.setSort(dto.getSort() == null ? 0 : dto.getSort());
        dept.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        dept.setCreateTime(LocalDateTime.now());
        dept.setUpdateTime(LocalDateTime.now());
        deptMapper.insert(dept);
        return dept.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, DeptDTO dto) {
        requireDept(id);
        if (id.equals(dto.getParentId())) {
            throw new BizException("上级部门不能是自己");
        }
        Dept dept = new Dept();
        BeanUtils.copyProperties(dto, dept);
        dept.setId(id);
        dept.setUpdateTime(LocalDateTime.now());
        deptMapper.updateById(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        requireDept(id);
        Long childCount = deptMapper.selectCount(Wrappers.<Dept>lambdaQuery().eq(Dept::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BizException("存在下级部门，不允许删除");
        }
        Long userCount = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getDeptId, id));
        if (userCount != null && userCount > 0) {
            throw new BizException("部门下存在用户，不允许删除");
        }
        deptMapper.deleteById(id);
    }

    @Override
    public List<DeptVO> listAll() {
        List<Dept> list = deptMapper.selectList(Wrappers.<Dept>lambdaQuery()
                .orderByAsc(Dept::getSort)
                .orderByAsc(Dept::getId));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<DeptVO> tree() {
        return buildTree(listAll(), 0L);
    }

    private List<DeptVO> buildTree(List<DeptVO> all, Long parentId) {
        List<DeptVO> result = new ArrayList<>();
        for (DeptVO node : all) {
            Long nodeParent = node.getParentId() == null ? 0L : node.getParentId();
            if (nodeParent.equals(parentId)) {
                node.setChildren(buildTree(all, node.getId()));
                result.add(node);
            }
        }
        return result;
    }

    private DeptVO toVO(Dept dept) {
        DeptVO vo = new DeptVO();
        BeanUtils.copyProperties(dept, vo);
        return vo;
    }

    private Dept requireDept(Long id) {
        if (id == null) {
            throw new BizException("部门ID不能为空");
        }
        Dept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BizException("部门不存在");
        }
        return dept;
    }
}
