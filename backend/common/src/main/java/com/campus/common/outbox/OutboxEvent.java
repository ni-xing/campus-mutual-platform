package com.campus.common.outbox;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Outbox 待投递事件（《系统设计》§4.2.t8，各业务库同构）。
 *
 * <p>业务服务在本地事务内写入本表（见 {@link OutboxTemplate}），
 * notify-service 通过原子领取 UPDATE 抢占投递。
 */
@Data
@TableName("t_outbox_event")
public class OutboxEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 事件唯一键（防重复投递） */
    private String eventId;

    /** 事件类型（Published Language Schema） */
    private String eventType;

    /** 业务聚合 ID */
    private String aggregateId;

    /** 接收人 */
    private Long targetUserId;

    /** JSON 业务载荷 */
    private String payload;

    /** INIT / PROCESSING / SENT / RETRY / DEAD */
    private String status;

    /** 领取实例标识（原子领取） */
    private String owner;

    /** 重试次数（≥ 5 → DEAD） */
    private Integer retryCount;

    /** 下次可领取时间（退避） */
    private LocalDateTime nextRetryTime;

    /** 投递成功时间（成功证据，V5） */
    private LocalDateTime sentTime;

    /** 事件发生时间 */
    private LocalDateTime createdTime;
}
