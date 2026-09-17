package com.campus.trade.service;

import com.campus.trade.common.CacheKeys;
import com.campus.trade.mapper.GoodsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 浏览量回写任务（specs/01 S-03）：Redis 自增计数每分钟 GETDEL 批量回写 MySQL，
 * 削峰高频详情页写压力；服务重启最多丢一个窗口的增量（浏览量非关键数据，可接受）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountFlushJob {

    private final StringRedisTemplate redis;
    private final GoodsMapper goodsMapper;

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void flush() {
        Map<Long, Long> pending = new HashMap<>();
        try (Cursor<String> cursor = redis.scan(
                ScanOptions.scanOptions().match(CacheKeys.GOODS_VIEWS_PREFIX).count(500).build())) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                // GETDEL：原子取走计数，回写失败时下个窗口重新累计（宁少勿重）
                String value = redis.opsForValue().getAndDelete(key);
                if (value == null) {
                    continue;
                }
                try {
                    long delta = Long.parseLong(value);
                    long goodsId = Long.parseLong(key.substring(CacheKeys.GOODS_VIEWS.length()));
                    if (delta > 0) {
                        pending.put(goodsId, delta);
                    }
                } catch (NumberFormatException e) {
                    log.warn("view count key malformed: {}", key);
                }
            }
        } catch (Exception e) {
            log.warn("view count scan failed", e);
            return;
        }
        pending.forEach((goodsId, delta) -> {
            try {
                goodsMapper.incrViewCount(goodsId, delta);
            } catch (Exception e) {
                log.warn("view count flush failed goodsId={} delta={}", goodsId, delta, e);
            }
        });
        if (!pending.isEmpty()) {
            log.debug("view count flushed: {} goods", pending.size());
        }
    }
}
