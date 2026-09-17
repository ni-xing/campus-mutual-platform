package com.campus.trade.controller;

import com.campus.common.dto.PageRequest;
import com.campus.common.dto.PageResponse;
import com.campus.common.dto.Result;
import com.campus.common.dto.UserContextHolder;
import com.campus.common.idempotent.Idempotent;
import com.campus.trade.dto.BalanceVO;
import com.campus.trade.dto.FlowVO;
import com.campus.trade.dto.RechargeRequest;
import com.campus.trade.service.BalanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 余额接口（specs/01 S-07；方案 B 模拟支付配套）。
 * 页面文案严禁出现"真实支付"暗示，前端统一标注"模拟环境"。
 */
@RestController
@RequestMapping("/api/v1/trade/balance")
@RequiredArgsConstructor
public class TradeBalanceController {

    private final BalanceService balanceService;

    @GetMapping
    public Result<BalanceVO> myBalance() {
        return Result.ok(balanceService.myBalance());
    }

    /** 模拟充值（点击即到账；单笔 ≤1000） */
    @PostMapping("/recharge")
    @Idempotent(message = "充值处理中，请勿重复提交")
    public Result<BalanceVO> recharge(@Valid @RequestBody RechargeRequest req) {
        balanceService.recharge(UserContextHolder.currentUserId(), req.getAmount());
        return Result.ok(balanceService.myBalance());
    }

    @GetMapping("/flows")
    public Result<PageResponse<FlowVO>> flows(@RequestParam(required = false) Integer pageNo,
                                              @RequestParam(required = false) Integer pageSize) {
        return Result.ok(balanceService.myFlows(new PageRequest(pageNo, pageSize)));
    }
}
