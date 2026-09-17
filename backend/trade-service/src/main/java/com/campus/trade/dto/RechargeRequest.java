package com.campus.trade.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** 模拟充值请求（方案 B：点击即到账，页面标注模拟环境；单笔 ≤ 1000）。 */
@Data
public class RechargeRequest {

    @NotNull
    @DecimalMin(value = "0.01", message = "充值金额必须大于 0")
    @DecimalMax(value = "1000", message = "单笔模拟充值上限 1000")
    private BigDecimal amount;
}
