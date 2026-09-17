package com.campus.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 评价 VO。 */
@Data
public class ReviewVO {

    private Long id;

    private Long orderId;

    private Long raterId;

    private Long rateeId;

    private Integer rating;

    private String content;

    private LocalDateTime createdTime;
}
