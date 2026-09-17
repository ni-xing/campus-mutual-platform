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
 * 二手商品主档（t_goods，单库存）。状态：LISTED/SOLD/OFFSHELF/DELETED。
 */
@Data
@TableName("t_goods")
public class Goods {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String schoolCode;

    private Long sellerId;

    private String title;

    private String description;

    /** 类目：BOOK/DIGITAL/DAILY/SPORT/OTHER */
    private String category;

    private BigDecimal price;

    /** 图片 URL JSON 数组（≤ 9） */
    private String images;

    private Integer stock;

    private String status;

    private String auditStatus;

    /** 浏览量（Redis 自增 + 定时回写，W2 新增列） */
    private Integer viewCount;

    @Version
    private Integer version;

    private String createdBy;

    private LocalDateTime createdTime;

    private String updatedBy;

    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;
}
