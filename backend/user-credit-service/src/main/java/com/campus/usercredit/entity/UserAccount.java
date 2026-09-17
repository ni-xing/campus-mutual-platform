package com.campus.usercredit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户账号与信用主档（t_user_account，《系统设计》§4.2.t1）。
 */
@Data
@TableName("t_user_account")
public class UserAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String schoolCode;

    private String studentNo;

    private String email;

    private String passwordHash;

    private String nickname;

    private String role;

    private String status;

    private Integer creditScore;

    @Version
    private Integer version;

    /** 隐私政策版本（F18 同意留痕） */
    private String privacyConsentVersion;

    /** 隐私同意勾选时间（F18） */
    private LocalDateTime privacyConsentTime;

    private String createdBy;

    private LocalDateTime createdTime;

    private String updatedBy;

    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;
}
