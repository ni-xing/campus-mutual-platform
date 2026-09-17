package com.campus.usercredit.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 注册请求（specs/05 U-01；UserStory US-1 RegisterRequest）。
 */
@Data
public class RegisterRequest {

    /** 学号，全校唯一，8~12 位数字 */
    @NotBlank
    @Pattern(regexp = "^[0-9]{8,12}$", message = "学号须为 8~12 位数字")
    private String studentNo;

    /** 校园邮箱（域名白名单校验在服务层） */
    @NotBlank
    @Pattern(regexp = "^[^@\s]+@[^@\s]+$", message = "邮箱格式不正确")
    private String email;

    /** 密码 >=8 位且非纯数字（安全设计 §2.1.2 冻结口径） */
    @NotBlank
    private String password;

    /** 昵称，可空则默认"同学+学号后4位" */
    private String nickname;

    /** 隐私同意（F18）：未勾选后端拒绝（前端按钮置灰为第一层） */
    @AssertTrue(message = "请先阅读并同意《用户协议与隐私政策》")
    private Boolean privacyAgreed;

    /** 勾选时的政策版本，缺省取服务端当前版本 */
    private String privacyVersion;
}
