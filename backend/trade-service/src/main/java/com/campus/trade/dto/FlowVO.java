package com.campus.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 余额流水 VO。 */
@Data
public class FlowVO {

    private Long id;

    private String flowType;

    private BigDecimal amount;

    /** 方向：1 增 0 减 */
    private Integer direction;

    private String bizRef;

    private BigDecimal balanceAfter;

    private BigDecimal frozenAfter;

    private LocalDateTime createdTime;
}
