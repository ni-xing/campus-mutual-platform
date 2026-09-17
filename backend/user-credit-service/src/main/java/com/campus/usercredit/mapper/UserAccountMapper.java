package com.campus.usercredit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.usercredit.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;

/** 用户账号 Mapper（MyBatis-Plus BaseMapper）。 */
@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {
}
