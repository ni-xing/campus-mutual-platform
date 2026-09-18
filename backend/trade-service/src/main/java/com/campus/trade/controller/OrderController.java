package com.campus.trade.controller;

import com.campus.common.dto.PageResponse;
import com.campus.common.dto.Result;
import com.campus.trade.dto.OrderCreateRequest;
import com.campus.trade.dto.OrderVO;
import com.campus.trade.dto.ReviewCreateRequest;
import com.campus.trade.dto.ReviewVO;
import com.campus.trade.service.OrderService;
import com.campus.trade.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单接口（specs/01 S-04~S-09）。
 *
 * <p>幂等策略：下单/评价**不在 Controller 挂 {@code @Idempotent}**——切面只返回
 * C000004「处理中」而拿不到原结果，与"同键重试返回同一单"语义冲突。改由业务层
 * 按业务键查重并返回原结果（幂等键本身落在 uk 约束上）：
 * <ul>
 *   <li>下单：X-Idempotency-Key → t_trade_order.uk_idem 预检，命中即返回原单</li>
 *   <li>评价：uk_order_rater 预检，命中抛 A020003「你已评价过本单」</li>
 * </ul>
 * 状态机：FROZEN →（确认取货）COMPLETED /（取消）CANCELLED。
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ReviewService reviewService;

    @PostMapping
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateRequest req,
                                  @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        return Result.ok(orderService.create(req, idempotencyKey));
    }

    /**
     * 我的订单列表。**必须显式声明字面量路径 {@code /mine}**：若只用根路径 {@code @GetMapping}，
     * 前端的 {@code GET /orders/mine} 会被 {@code @GetMapping("/{id}")} 抢先匹配，
     * {@code "mine"} 转 Long 抛 NumberFormatException → B000001（曾导致订单页整体不可用）。
     */
    @GetMapping({"/mine", ""})
    public Result<PageResponse<OrderVO>> myOrders(@RequestParam(required = false) Integer pageNo,
                                                  @RequestParam(required = false) Integer pageSize,
                                                  @RequestParam(defaultValue = "buyer") String role) {
        return Result.ok(orderService.myOrders(pageNo, pageSize, role));
    }

    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.detail(id));
    }

    /** 确认取货（完成订单，结算划款） */
    @PostMapping("/{id}/complete")
    public Result<OrderVO> complete(@PathVariable Long id) {
        return Result.ok(orderService.complete(id));
    }

    /** 取消订单（解冻退款 + 商品回架） */
    @PostMapping("/{id}/cancel")
    public Result<OrderVO> cancel(@PathVariable Long id) {
        return Result.ok(orderService.cancel(id));
    }

    /** 评价（订单完成后 7 天内，双方各一次；重复评价返回 A020003） */
    @PostMapping("/{id}/review")
    public Result<ReviewVO> review(@PathVariable Long id, @Valid @RequestBody ReviewCreateRequest req) {
        return Result.ok(reviewService.create(id, req));
    }

    @GetMapping("/{id}/reviews")
    public Result<List<ReviewVO>> reviews(@PathVariable Long id) {
        return Result.ok(reviewService.listByOrder(id));
    }
}
