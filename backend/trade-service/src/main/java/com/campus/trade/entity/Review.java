package com.campus.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 双向评价（t_review，永久保留）。每人每订单仅一次（uk_order_rater）。
 */
@Data
@TableName("t_review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String schoolCode;

    private Long orderId;

    /** 订单类型：TRADE/ERRAND */
    private String orderType;

    private Long raterId;

    private Long rateeId;

    /** 评分 1~5 */
    private Integer rating;

    private String content;

    private String createdBy;

    private LocalDateTime createdTime;

    private String updatedBy;

    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;
}
