package com.campus.usercredit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 登录/注册成功返回：Token + 用户信息（系统设计 SQ-11）。 */
@Data
@AllArgsConstructor
public class LoginVO {
    private String token;
    private UserVO user;
}
