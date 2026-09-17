package com.campus.usercredit.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 信用分回流请求（specs/05 U-07 / 系统设计 §3.2.M1.3）。
 *
 * <p>幂等键 bizEventId（DB 唯一索引兜底）；delta 约定：
 * 评星映射 1星-5/2星-2/3星+1/4星+3/5星+5，违约 -10。
 */
@Data
public class CreditAdjustRequest {

    @NotBlank
    private String bizEventId;

    @NotNull
    private Long userId;

    @NotNull
    @Min(-100)
    @Max(100)
    private Integer delta;

    /** 来源：REVIEW / FULFILL / DEFAULT */
    @NotBlank
    private String sourceType;
}
