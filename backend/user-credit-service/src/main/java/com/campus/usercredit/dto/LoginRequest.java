package com.campus.usercredit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求（specs/05 U-02）：学号或校园邮箱 + 密码。
 */
@Data
public class LoginRequest {

    @NotBlank
    private String account;

    @NotBlank
    private String password;
}
