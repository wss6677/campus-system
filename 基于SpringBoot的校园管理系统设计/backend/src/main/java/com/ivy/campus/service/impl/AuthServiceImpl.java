package com.ivy.campus.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ivy.campus.common.BizException;
import com.ivy.campus.common.Constants;
import com.ivy.campus.dto.LoginDTO;
import com.ivy.campus.dto.RegisterDTO;
import com.ivy.campus.entity.Dept;
import com.ivy.campus.entity.Role;
import com.ivy.campus.entity.User;
import com.ivy.campus.mapper.DeptMapper;
import com.ivy.campus.mapper.RoleMapper;
import com.ivy.campus.mapper.UserMapper;
import com.ivy.campus.security.JwtUtil;
import com.ivy.campus.security.LoginUser;
import com.ivy.campus.security.UserContext;
import com.ivy.campus.service.AuthService;
import com.ivy.campus.vo.LoginVO;
import com.ivy.campus.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final String PWD_SALT = "campusPulse@2025";

    private final UserMapper userMapper;

    private final DeptMapper deptMapper;

    private final RoleMapper roleMapper;

    private final JwtUtil jwtUtil;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BizException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已停用，请联系管理员");
        }
        String encoded = encodePassword(dto.getPassword(), dto.getUsername());
        if (!encoded.equalsIgnoreCase(user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRoleCode());
        long expireSeconds = jwtUtil.getExpire() == null ? 86400L : jwtUtil.getExpire();
        LocalDateTime now = LocalDateTime.now();
        User update = new User();
        update.setId(user.getId());
        update.setLastLoginTime(now);
        userMapper.updateById(update);
        user.setLastLoginTime(now);
        try {
            stringRedisTemplate.opsForValue().set(Constants.REDIS_TOKEN_PREFIX + user.getId(), token,
                    expireSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("登录令牌写入Redis失败，忽略：{}", e.getMessage());
        }
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setTokenType("Bearer");
        vo.setExpiresIn(expireSeconds);
        vo.setUser(toUserVO(user));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO dto) {
        Long count = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, dto.getUsername()));
        if (count != null && count > 0) {
            throw new BizException("用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(encodePassword(dto.getPassword(), dto.getUsername()));
        user.setRealName(dto.getRealName());
        user.setNickname(dto.getNickname() == null || dto.getNickname().isBlank() ? dto.getRealName() : dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setGender(dto.getGender() == null ? 0 : dto.getGender());
        user.setDeptId(dto.getDeptId());
        user.setStudentNo(dto.getStudentNo());
        user.setUserType(dto.getUserType() == null || dto.getUserType().isBlank() ? "STUDENT" : dto.getUserType());
        user.setRoleCode("STUDENT");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
    }

    @Override
    public Map<String, String> captcha() {
        Map<String, String> map = new HashMap<>(4);
        String uuid = IdUtil.fastSimpleUUID();
        String code = RandomUtil.randomNumbers(4);
        try {
            stringRedisTemplate.opsForValue().set(Constants.REDIS_TOKEN_PREFIX + "captcha:" + uuid, code, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("验证码写入Redis失败，忽略：{}", e.getMessage());
        }
        map.put("uuid", uuid);
        map.put("code", code);
        return map;
    }

    @Override
    public UserVO info() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException("未登录或登录已过期");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return toUserVO(user);
    }

    @Override
    public void logout() {
        LoginUser loginUser = UserContext.get();
        if (loginUser == null) {
            return;
        }
        try {
            stringRedisTemplate.delete(Constants.REDIS_TOKEN_PREFIX + loginUser.getUserId());
        } catch (Exception e) {
            log.warn("退出登录清理Redis失败，忽略：{}", e.getMessage());
        } finally {
            UserContext.clear();
        }
    }

    private String encodePassword(String rawPassword, String username) {
        return DigestUtil.md5Hex(rawPassword + PWD_SALT + username);
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        if (user.getDeptId() != null) {
            Dept dept = deptMapper.selectById(user.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
        }
        if (user.getRoleCode() != null) {
            Role role = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getCode, user.getRoleCode()));
            if (role != null) {
                vo.setRoleName(role.getName());
            }
        }
        return vo;
    }
}
