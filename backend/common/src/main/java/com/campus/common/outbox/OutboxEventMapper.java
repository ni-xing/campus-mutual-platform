package com.campus.common.outbox;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** Outbox 事件表 Mapper（各业务服务扫描到自己的库）。 */
@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEvent> {
}
