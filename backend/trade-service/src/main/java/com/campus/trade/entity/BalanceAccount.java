package com.campus.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 余额账户主档（t_balance_account，注册即开户；交易侧懒创建）。
 * points 为 W2 新增列：交易积分（订单金额 1% 向下取整）。
 */
@Data
@TableName("t_balance_account")
public class BalanceAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String schoolCode;

    private Long userId;

    private BigDecimal balance;

    private BigDecimal frozen;

    /** 交易积分（W2 新增列） */
    private Integer points;

    @Version
    private Integer version;

    private String createdBy;

    private LocalDateTime createdTime;

    private String updatedBy;

    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;
}
