package com.campus.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 商品 VO（列表/详情共用；detail 额外带卖家昵称与浏览量）。 */
@Data
public class GoodsVO {

    private Long id;

    private Long sellerId;

    /** 详情页 enrich（内部接口查询，失败降级为空） */
    private String sellerNickname;

    private Integer sellerCreditScore;

    private String title;

    private String description;

    private String category;

    private BigDecimal price;

    private List<String> images;

    private String status;

    private Integer viewCount;

    private LocalDateTime createdTime;
}
