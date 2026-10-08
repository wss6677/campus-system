package com.ivy.campus.service;

import com.ivy.campus.common.PageResult;
import com.ivy.campus.dto.UserDTO;
import com.ivy.campus.vo.UserVO;

import java.util.List;

public interface UserService {

    PageResult<UserVO> page(Integer pageNum, Integer pageSize, String keyword, String roleCode, Long deptId, Integer status);

    UserVO getById(Long id);

    Long save(UserDTO dto);

    void update(Long id, UserDTO dto);

    void remove(Long id);

    void resetPwd(Long id, String password);

    List<UserVO> listAll();

    // 按角色码取启用用户ID集合
    List<Long> listUserIdsByRole(String roleCode);

    // 按部门取启用用户ID集合
    List<Long> listUserIdsByDept(Long deptId);
}
