package com.campus.usercredit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 信用分增减明细（t_credit_record，§4.2.10）；biz_event_id 唯一索引为回流幂等兜底。
 */
@Data
@TableName("t_credit_record")
public class CreditRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String schoolCode;

    private Long userId;

    /** 本次增减值（可负） */
    private Integer delta;

    /** 来源：REVIEW / FULFILL / DEFAULT */
    private String sourceType;

    /** 变动后信用分快照 */
    private Integer scoreAfter;

    /** 业务事件键（幂等，uk） */
    private String bizEventId;

    private String createdBy;

    private LocalDateTime createdTime;

    private String updatedBy;

    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;
}
