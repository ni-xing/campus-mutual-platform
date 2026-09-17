package com.campus.trade.feign;

import com.campus.common.dto.Result;
import com.campus.trade.dto.UserSnapshotVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * M1 用户与信用服务内部客户端（OpenFeign + Nacos lb）。
 *
 * <p>仅 docker 内网可达（/internal/** 网关 404）；调用失败由调用方降级，
 * 不阻塞下单主流程（昵称快照失败回落"同学{userId}"）。
 */
@FeignClient(name = "user-credit-service", contextId = "internalUserClient")
public interface UserClient {

    @GetMapping("/internal/users/{userId}")
    Result<UserSnapshotVO> snapshot(@PathVariable("userId") Long userId);

    /** 快照查询（异常/非成功码一律降级返回 null，调用方自行兜底） */
    default UserSnapshotVO snapshotSafe(Long userId) {
        try {
            Result<UserSnapshotVO> result = snapshot(userId);
            if (result != null && result.success() && result.data() != null) {
                return result.data();
            }
        } catch (Exception ignored) {
            // 降级：M1 不可用不阻塞交易主流程
        }
        return null;
    }
}
