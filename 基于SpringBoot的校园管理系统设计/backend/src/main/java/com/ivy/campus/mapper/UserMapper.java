package com.ivy.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ivy.campus.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
