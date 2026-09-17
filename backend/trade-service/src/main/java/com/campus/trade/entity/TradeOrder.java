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
 * 二手交易订单（t_trade_order，永久保留）。
 *
 * <p>状态机（DDL 口径，冻结式资金流）：下单事务内一步到 FROZEN（无 CREATED 落库态）→
 * 确认取货 COMPLETED（结算划款）/ 取消 CANCELLED（解冻退款）。非法流转由状态机拒绝（A020004）。
 */
@Data
@TableName("t_trade_order")
public class TradeOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String schoolCode;

    private String orderNo;

    private Long goodsId;

    /** 商品标题快照 */
    private String goodsTitle;

    /** 成交价快照 */
    private BigDecimal goodsPrice;

    private Long buyerId;

    /** 买家昵称快照（下单时经内部接口取自 M1） */
    private String buyerNickname;

    private Long sellerId;

    private String status;

    /** 下单幂等键（DB 兜底防线，uk_idem） */
    private String idempotencyKey;

    private String remark;

    /** 评价窗口截止（完成 + 7 天） */
    private LocalDateTime reviewDeadline;

    @Version
    private Integer version;

    private String createdBy;

    private LocalDateTime createdTime;

    private String updatedBy;

    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;
}
