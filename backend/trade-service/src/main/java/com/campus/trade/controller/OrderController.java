package com.campus.trade.controller;

import com.campus.common.dto.PageResponse;
import com.campus.common.dto.Result;
import com.campus.common.idempotent.Idempotent;
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
 * <p>下单为资金类操作，强制 @Idempotent（X-Idempotency-Key），DB uk_idem 为最终防线。
 * 状态机：FROZEN →（确认取货）COMPLETED /（取消）CANCELLED。
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ReviewService reviewService;

    @PostMapping
    @Idempotent(message = "下单处理中，请勿重复提交")
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateRequest req,
                                  @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        return Result.ok(orderService.create(req, idempotencyKey));
    }

    @GetMapping
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

    /** 评价（订单完成后 7 天内，双方各一次） */
    @PostMapping("/{id}/review")
    @Idempotent(message = "评价处理中，请勿重复提交")
    public Result<ReviewVO> review(@PathVariable Long id, @Valid @RequestBody ReviewCreateRequest req) {
        return Result.ok(reviewService.create(id, req));
    }

    @GetMapping("/{id}/reviews")
    public Result<List<ReviewVO>> reviews(@PathVariable Long id) {
        return Result.ok(reviewService.listByOrder(id));
    }
}
