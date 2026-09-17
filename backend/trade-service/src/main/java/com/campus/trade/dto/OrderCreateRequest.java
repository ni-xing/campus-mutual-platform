package com.campus.trade.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 下单请求（specs/01 S-04/S-05：下单即冻结余额 = 模拟支付，页面标注模拟环境）。 */
@Data
public class OrderCreateRequest {

    @NotNull
    private Long goodsId;

    @Size(max = 100)
    private String remark;
}
