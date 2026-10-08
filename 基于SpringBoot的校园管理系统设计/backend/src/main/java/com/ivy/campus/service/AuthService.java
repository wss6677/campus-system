package com.ivy.campus.service;

import com.ivy.campus.dto.LoginDTO;
import com.ivy.campus.dto.RegisterDTO;
import com.ivy.campus.vo.LoginVO;
import com.ivy.campus.vo.UserVO;

import java.util.Map;

public interface AuthService {

    // 登录：校验密码、签发token、更新最后登录时间
    LoginVO login(LoginDTO dto);

    // 注册：默认学生角色，密码加盐存储
    void register(RegisterDTO dto);

    // 模拟验证码，返回 uuid 与 code
    Map<String, String> captcha();

    // 当前登录人信息
    UserVO info();

    // 退出登录，清空token缓存
    void logout();
}
