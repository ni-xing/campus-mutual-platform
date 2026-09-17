package com.campus.usercredit.dto;

import lombok.Data;

/** 他人主页视图：仅公开信息，不含邮箱/学号原文。 */
@Data
public class UserPublicVO {
    private Long id;
    private String nickname;
    private String studentNoMasked;
    private Integer creditScore;
}
