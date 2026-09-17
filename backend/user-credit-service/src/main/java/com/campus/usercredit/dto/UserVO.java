package com.campus.usercredit.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 本人主页视图（specs/05 U-06，学号脱敏）。 */
@Data
public class UserVO {
    private Long id;
    private String nickname;
    private String schoolCode;
    /** 学号脱敏展示（中间 4 位打星） */
    private String studentNoMasked;
    private String email;
    private String status;
    private Integer creditScore;
    private LocalDateTime createdAt;
}
