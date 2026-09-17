package com.campus.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 余额流水（t_balance_flow，资金幂等与对账核心，永久保留）。
 * 类型：FREEZE/UNFREEZE/DEDUCT/INCOME/RECHARGE/REWARD；方向：1 增 0 减。
 */
@Data
@TableName("t_balance_flow")
public class BalanceFlow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String schoolCode;

    private Long userId;

    private String flowType;

    /** 本笔金额（正数） */
    private BigDecimal amount;

    /** 方向：1 增 0 减 */
    private Integer direction;

    /** 业务请求号（幂等键，uk_biz_req） */
    private String bizReqNo;

    /** 关联业务单号（订单/跑腿单） */
    private String bizRef;

    /** 变动后可用余额快照 */
    private BigDecimal balanceAfter;

    /** 变动后冻结额快照 */
    private BigDecimal frozenAfter;

    private String createdBy;

    private LocalDateTime createdTime;

    private String updatedBy;

    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;
}
