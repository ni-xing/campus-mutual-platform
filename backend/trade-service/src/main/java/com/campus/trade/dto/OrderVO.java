package com.campus.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订单 VO。 */
@Data
public class OrderVO {

    private Long id;

    private String orderNo;

    private Long goodsId;

    private String goodsTitle;

    private BigDecimal goodsPrice;

    private Long buyerId;

    private String buyerNickname;

    private Long sellerId;

    private String status;

    private String remark;

    /** 当前用户是否已评价（订单列表/详情 enrich） */
    private Boolean reviewedByMe;

    private LocalDateTime reviewDeadline;

    private LocalDateTime createdTime;
}
