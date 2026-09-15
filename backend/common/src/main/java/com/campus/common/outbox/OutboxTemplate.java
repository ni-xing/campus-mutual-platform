package com.campus.common.outbox;

import com.campus.common.enums.OutboxStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbox 写入组件（《系统设计》§2.2.2 C-05 / §4.2.t8）。
 *
 * <p>必须在业务本地事务内调用：与业务数据同事务写入，保证"业务成功则事件必存在"，
 * 是 V5「通知投递成功率 ≥ 99%」的源头保障。
 */
@Slf4j
@RequiredArgsConstructor
public class OutboxTemplate {

    private final OutboxEventMapper outboxEventMapper;
    private final ObjectMapper objectMapper;

    public void publish(String eventType, String aggregateId, Long targetUserId, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("outbox payload serialize failed: " + eventType, e);
        }
        if (json.length() > 1024) {
            // payload 列 VARCHAR(1024)，超长直接截断前落日志，避免事务因字段溢出失败
            log.warn("outbox payload truncated, eventType={} len={}", eventType, json.length());
            json = json.substring(0, 1024);
        }
        OutboxEvent event = new OutboxEvent();
        event.setEventId(UUID.randomUUID().toString().replace("-", ""));
        event.setEventType(eventType);
        event.setAggregateId(aggregateId);
        event.setTargetUserId(targetUserId);
        event.setPayload(json);
        event.setStatus(OutboxStatus.INIT.name());
        event.setRetryCount(0);
        event.setCreatedTime(LocalDateTime.now());
        outboxEventMapper.insert(event);
    }
}
