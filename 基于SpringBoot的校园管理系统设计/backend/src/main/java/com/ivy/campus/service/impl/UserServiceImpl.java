package com.ivy.campus.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ivy.campus.common.BizException;
import com.ivy.campus.common.PageResult;
import com.ivy.campus.dto.UserDTO;
import com.ivy.campus.entity.Dept;
import com.ivy.campus.entity.Role;
import com.ivy.campus.entity.User;
import com.ivy.campus.mapper.DeptMapper;
import com.ivy.campus.mapper.RoleMapper;
import com.ivy.campus.mapper.UserMapper;
import com.ivy.campus.service.UserService;
import com.ivy.campus.util.PageUtils;
import com.ivy.campus.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private static final String PWD_SALT = "campusPulse@2025";

    private static final String DEFAULT_PASSWORD = "123456";

    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final RoleMapper roleMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public PageResult<UserVO> page(Integer pageNum, Integer pageSize, String keyword, String roleCode, Long deptId,
                                   Integer status) {
        IPage<User> page = userMapper.selectPage(PageUtils.of(pageNum, pageSize),
                Wrappers.<User>lambdaQuery()
                        .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                                .like(User::getUsername, keyword)
                                .or().like(User::getRealName, keyword)
                                .or().like(User::getStudentNo, keyword))
                        .eq(roleCode != null && !roleCode.isBlank(), User::getRoleCode, roleCode)
                        .eq(deptId != null, User::getDeptId, deptId)
                        .eq(status != null, User::getStatus, status)
                        .orderByDesc(User::getId));
        List<UserVO> records = convertList(page.getRecords());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public UserVO getById(Long id) {
        User user = requireUser(id);
        return convert(user, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(UserDTO dto) {
        Long count = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, dto.getUsername()));
        if (count != null && count > 0) {
            throw new BizException("用户名已存在");
        }
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        user.setId(null);
        String rawPassword = dto.getPassword() == null || dto.getPassword().isBlank()
                ? DEFAULT_PASSWORD : dto.getPassword();
        user.setPassword(encodePassword(rawPassword, dto.getUsername()));
        user.setGender(dto.getGender() == null ? 0 : dto.getGender());
        user.setUserType(dto.getUserType() == null || dto.getUserType().isBlank() ? "STAFF" : dto.getUserType());
        user.setRoleCode(dto.getRoleCode() == null || dto.getRoleCode().isBlank() ? "TEACHER" : dto.getRoleCode());
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UserDTO dto) {
        User exists = requireUser(id);
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        user.setId(id);
        user.setDeleted(null);
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            user.setPassword(null);
        } else {
            String username = dto.getUsername() == null || dto.getUsername().isBlank()
                    ? exists.getUsername() : dto.getUsername();
            user.setPassword(encodePassword(dto.getPassword(), username));
            clearTokenCache(id);
        }
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        requireUser(id);
        userMapper.deleteById(id);
        clearTokenCache(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPwd(Long id, String password) {
        User exists = requireUser(id);
        String rawPassword = password == null || password.isBlank() ? DEFAULT_PASSWORD : password;
        User update = new User();
        update.setId(id);
        update.setPassword(encodePassword(rawPassword, exists.getUsername()));
        update.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(update);
        clearTokenCache(id);
        log.info("用户密码已重置，userId={}", id);
    }

    @Override
    public List<UserVO> listAll() {
        List<User> users = userMapper.selectList(Wrappers.<User>lambdaQuery().orderByAsc(User::getId));
        return convertList(users);
    }

    @Override
    public List<Long> listUserIdsByRole(String roleCode) {
        List<User> users = userMapper.selectList(Wrappers.<User>lambdaQuery()
                .eq(roleCode != null && !roleCode.isBlank(), User::getRoleCode, roleCode)
                .eq(User::getStatus, 1));
        return users.stream().map(User::getId).collect(Collectors.toList());
    }

    @Override
    public List<Long> listUserIdsByDept(Long deptId) {
        List<User> users = userMapper.selectList(Wrappers.<User>lambdaQuery()
                .eq(deptId != null, User::getDeptId, deptId)
                .eq(User::getStatus, 1));
        return users.stream().map(User::getId).collect(Collectors.toList());
    }

    private List<UserVO> convertList(List<User> users) {
        List<UserVO> result = new ArrayList<>();
        if (users == null || users.isEmpty()) {
            return result;
        }
        Map<Long, Dept> deptMap = loadDeptMap(users);
        Map<String, Role> roleMap = loadRoleMap();
        for (User user : users) {
            result.add(convert(user, deptMap, roleMap));
        }
        return result;
    }

    private UserVO convert(User user, Map<Long, Dept> deptMap, Map<String, Role> roleMap) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        if (user.getDeptId() != null) {
            Dept dept = deptMap == null ? null : deptMap.get(user.getDeptId());
            if (dept == null) {
                dept = deptMapper.selectById(user.getDeptId());
            }
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
        }
        if (user.getRoleCode() != null) {
            Role role = roleMap == null ? null : roleMap.get(user.getRoleCode());
            if (role == null) {
                role = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getCode, user.getRoleCode()));
            }
            if (role != null) {
                vo.setRoleName(role.getName());
            }
        }
        return vo;
    }

    private Map<Long, Dept> loadDeptMap(List<User> users) {
        Map<Long, Dept> map = new HashMap<>();
        List<Long> ids = users.stream().map(User::getDeptId).filter(Objects::nonNull).distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return map;
        }
        for (Dept dept : deptMapper.selectBatchIds(ids)) {
            map.put(dept.getId(), dept);
        }
        return map;
    }

    private Map<String, Role> loadRoleMap() {
        Map<String, Role> map = new HashMap<>();
        for (Role role : roleMapper.selectList(Wrappers.<Role>lambdaQuery())) {
            map.put(role.getCode(), role);
        }
        return map;
    }

    private User requireUser(Long id) {
        if (id == null) {
            throw new BizException("用户ID不能为空");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return user;
    }

    private String encodePassword(String rawPassword, String username) {
        return DigestUtil.md5Hex(rawPassword + PWD_SALT + username);
    }

    private void clearTokenCache(Long userId) {
        try {
            Set<String> keys = stringRedisTemplate.keys("campus:token:" + userId + "*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("清理登录令牌缓存失败，userId={}，原因：{}", userId, e.getMessage());
        }
    }
}
