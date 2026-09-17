package com.campus.usercredit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录流水（t_login_log，§7.2.4，保留 1 年）。
 */
@Data
@TableName("t_login_log")
public class LoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String schoolCode;

    /** 用户 ID（失败且账号不存在时为空） */
    private Long userId;

    /** 登录标识（脱敏展示） */
    private String studentNo;

    private String ip;

    private String userAgent;

    /** 结果：SUCCESS / FAIL / LOCKED */
    private String result;

    private LocalDateTime createdTime;
}
