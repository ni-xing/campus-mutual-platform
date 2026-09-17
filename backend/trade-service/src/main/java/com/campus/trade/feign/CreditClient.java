package com.campus.trade.feign;

import com.campus.common.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * M1 信用回流内部客户端：评价星 → 信用分（1:-5/2:-2/3:+1/4:+3/5:+5，违约 -10）。
 * 调用失败仅记日志不回滚交易（信用回流最终一致，MVP 可手工/后续重试补偿）。
 */
@FeignClient(name = "user-credit-service", contextId = "internalCreditClient")
public interface CreditClient {

    @PostMapping("/internal/credit/adjust")
    Result<Void> adjust(@RequestBody Map<String, Object> body);
}
