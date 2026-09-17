package com.campus.trade.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 余额账户 VO（含 W2 交易积分）。 */
@Data
public class BalanceVO {

    private BigDecimal balance;

    private BigDecimal frozen;

    private Integer points;
}
