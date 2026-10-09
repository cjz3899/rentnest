package com.rentnest.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rentnest.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
