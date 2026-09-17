package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.dto.PageRequest;
import com.campus.common.dto.PageResponse;
import com.campus.common.dto.UserContextHolder;
import com.campus.common.enums.OrderStatus;
import com.campus.common.exception.BizException;
import com.campus.common.exception.ErrorCode;
import com.campus.common.outbox.OutboxTemplate;
import com.campus.trade.dto.OrderCreateRequest;
import com.campus.trade.dto.OrderVO;
import com.campus.trade.entity.Goods;
import com.campus.trade.entity.Review;
import com.campus.trade.entity.TradeOrder;
import com.campus.trade.feign.UserClient;
import com.campus.trade.mapper.GoodsMapper;
import com.campus.trade.mapper.ReviewMapper;
import com.campus.trade.mapper.TradeOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单服务（specs/01 S-04~S-08；状态机 DDL 口径：FROZEN → COMPLETED / CANCELLED）。
 *
 * <p>下单 = 模拟支付（方案 B）：同一事务内 条件占用商品（防超卖，AC-3）→ 冻结买家余额
 * （AC-2）→ 订单 FROZEN → Outbox 事件。完成 = 确认取货，结算划款；取消 = 解冻退款回架。
 * 非法流转一律 A020004 状态机拒绝（AC-5）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TradeOrderMapper orderMapper;
    private final GoodsMapper goodsMapper;
    private final ReviewMapper reviewMapper;
    private final BalanceService balanceService;
    private final GoodsService goodsService;
    private final UserClient userClient;
    private final OutboxTemplate outboxTemplate;

    /** 下单（S-04/S-05）：下单即冻结余额，无独立支付步骤；幂等键取 X-Idempotency-Key */
    @Transactional
    public OrderVO create(OrderCreateRequest req, String idempotencyKey) {
        Long userId = UserContextHolder.currentUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        String key = (idempotencyKey == null || idempotencyKey.isBlank())
                ? UUID.randomUUID().toString().replace("-", "") : idempotencyKey;
        // 0. 幂等预检（uk_idem）：同键重复请求直接返回已存在订单（任何资金写入之前）
        TradeOrder existed = orderMapper.selectOne(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getIdempotencyKey, key));
        if (existed != null) {
            return toVO(existed, userId);
        }
        Goods goods = goodsService.requireGoods(req.getGoodsId());
        if (goods.getSellerId().equals(userId)) {
            throw new BizException(ErrorCode.BUY_OWN_GOODS);
        }
        String orderNo = "TO" + LocalDateTime.now().format(ORDER_NO_FMT)
                + String.format("%04d", new Random().nextInt(10000));
        // 1. 条件占用商品（仅 LISTED → SOLD，AC-3 防超卖核心）
        if (goodsMapper.occupyIfListed(goods.getId()) != 1) {
            throw new BizException(ErrorCode.GOODS_UNAVAILABLE);
        }
        // 2. 冻结买家余额（余额不足抛 A020002，AC-2；异常时事务回滚含商品占用）
        balanceService.freezeForOrder(userId, goods.getPrice(), orderNo);
        // 3. 订单落库（快照 + 幂等键）
        TradeOrder order = new TradeOrder();
        order.setOrderNo(orderNo);
        order.setGoodsId(goods.getId());
        order.setGoodsTitle(goods.getTitle());
        order.setGoodsPrice(goods.getPrice());
        order.setBuyerId(userId);
        order.setBuyerNickname(nicknameSafe(userId));
        order.setSellerId(goods.getSellerId());
        order.setStatus(OrderStatus.FROZEN.name());
        order.setIdempotencyKey(key);
        order.setRemark(req.getRemark());
        order.setCreatedBy("user:" + userId);
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            // 并发同键兜底：uk_idem 冲突 → 抛业务异常回滚（含冻结资金），前端退避重查
            throw new BizException(ErrorCode.RATE_LIMITED, "下单处理中，请稍后刷新查看");
        }
        // 4. Outbox：通知卖家有新订单（通知服务 W5 消费）
        outboxTemplate.publish("TRADE_ORDER_CREATED", order.getOrderNo(), order.getSellerId(),
                Map.of("orderNo", order.getOrderNo(), "goodsTitle", order.getGoodsTitle(),
                        "amount", order.getGoodsPrice(), "buyerId", userId));
        log.info("ORDER_CREATED orderNo={} goodsId={} buyer={} amount={}",
                order.getOrderNo(), goods.getId(), userId, goods.getPrice());
        return toVO(orderMapper.selectById(order.getId()), userId);
    }

    /** 确认取货（S-06）：FROZEN → COMPLETED，结算划款 + 双方积分 + 评价窗口开启 */
    @Transactional
    public OrderVO complete(Long orderId) {
        Long userId = UserContextHolder.currentUserId();
        TradeOrder order = requireOrder(orderId);
        requireParticipant(order, userId);
        // 状态机守卫：仅 FROZEN → COMPLETED 合法，0 行即非法流转/并发冲突（AC-5）
        if (orderMapper.transition(orderId, OrderStatus.FROZEN.name(), OrderStatus.COMPLETED.name(),
                LocalDateTime.now().plusDays(7), "user:" + userId) != 1) {
            throw new BizException(ErrorCode.ORDER_STATE_INVALID);
        }
        balanceService.settle(order.getBuyerId(), order.getSellerId(), order.getGoodsPrice(), order.getOrderNo());
        outboxTemplate.publish("TRADE_ORDER_COMPLETED", order.getOrderNo(),
                userId.equals(order.getBuyerId()) ? order.getSellerId() : order.getBuyerId(),
                Map.of("orderNo", order.getOrderNo(), "goodsTitle", order.getGoodsTitle(),
                        "amount", order.getGoodsPrice()));
        log.info("ORDER_COMPLETED orderNo={} operator={}", order.getOrderNo(), userId);
        return toVO(orderMapper.selectById(orderId), userId);
    }

    /** 取消退款（S-08）：FROZEN → CANCELLED，解冻原路退回 + 商品回架（AC-4） */
    @Transactional
    public OrderVO cancel(Long orderId) {
        Long userId = UserContextHolder.currentUserId();
        TradeOrder order = requireOrder(orderId);
        requireParticipant(order, userId);
        // 状态机守卫：仅 FROZEN 可取消（AC-5：完成/取消后的单再操作一律拒绝）
        if (orderMapper.transition(orderId, OrderStatus.FROZEN.name(), OrderStatus.CANCELLED.name(),
                null, "user:" + userId) != 1) {
            throw new BizException(ErrorCode.ORDER_STATE_INVALID);
        }
        balanceService.unfreezeForOrder(order.getBuyerId(), order.getGoodsPrice(), order.getOrderNo());
        if (goodsMapper.restoreToListed(order.getGoodsId()) != 1) {
            log.warn("goods restore failed (not SOLD), goodsId={}", order.getGoodsId());
        }
        outboxTemplate.publish("TRADE_ORDER_CANCELLED", order.getOrderNo(),
                userId.equals(order.getBuyerId()) ? order.getSellerId() : order.getBuyerId(),
                Map.of("orderNo", order.getOrderNo(), "goodsTitle", order.getGoodsTitle()));
        log.info("ORDER_CANCELLED orderNo={} operator={}", order.getOrderNo(), userId);
        return toVO(orderMapper.selectById(orderId), userId);
    }

    /** 我的订单（role=buyer/seller） */
    public PageResponse<OrderVO> myOrders(Integer pageNo, Integer pageSize, String role) {
        Long userId = UserContextHolder.currentUserId();
        PageRequest pageRequest = new PageRequest(pageNo, pageSize);
        LambdaQueryWrapper<TradeOrder> wrapper = "seller".equalsIgnoreCase(role)
                ? new LambdaQueryWrapper<TradeOrder>().eq(TradeOrder::getSellerId, userId)
                : new LambdaQueryWrapper<TradeOrder>().eq(TradeOrder::getBuyerId, userId);
        wrapper.orderByDesc(TradeOrder::getCreatedTime);
        Page<TradeOrder> page = orderMapper.selectPage(
                new Page<>(pageRequest.safePageNo(), pageRequest.safePageSize()), wrapper);
        List<TradeOrder> orders = page.getRecords();
        Map<Long, Boolean> reviewedMap = reviewedByMeMap(userId, orders);
        List<OrderVO> records = orders.stream()
                .map(o -> {
                    OrderVO vo = toVO(o, userId);
                    vo.setReviewedByMe(reviewedMap.getOrDefault(o.getId(), false));
                    return vo;
                }).toList();
        return PageResponse.of(page.getTotal(), pageRequest.safePageSize(), records);
    }

    public OrderVO detail(Long orderId) {
        Long userId = UserContextHolder.currentUserId();
        TradeOrder order = requireOrder(orderId);
        requireParticipant(order, userId);
        OrderVO vo = toVO(order, userId);
        vo.setReviewedByMe(reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, orderId).eq(Review::getRaterId, userId)) > 0);
        return vo;
    }

    private Map<Long, Boolean> reviewedByMeMap(Long userId, List<TradeOrder> orders) {
        if (orders.isEmpty()) {
            return Map.of();
        }
        Set<Long> orderIds = orders.stream().map(TradeOrder::getId).collect(Collectors.toSet());
        return reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                        .in(Review::getOrderId, orderIds).eq(Review::getRaterId, userId))
                .stream().map(Review::getOrderId)
                .collect(Collectors.toMap(Function.identity(), id -> true));
    }

    private TradeOrder requireOrder(Long orderId) {
        TradeOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "订单不存在");
        }
        return order;
    }

    private void requireParticipant(TradeOrder order, Long userId) {
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }

    private String nicknameSafe(Long userId) {
        var snapshot = userClient.snapshotSafe(userId);
        return snapshot != null && snapshot.nickname() != null ? snapshot.nickname() : "同学" + userId;
    }

    private OrderVO toVO(TradeOrder order, Long userId) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setGoodsId(order.getGoodsId());
        vo.setGoodsTitle(order.getGoodsTitle());
        vo.setGoodsPrice(order.getGoodsPrice());
        vo.setBuyerId(order.getBuyerId());
        vo.setBuyerNickname(order.getBuyerNickname());
        vo.setSellerId(order.getSellerId());
        vo.setStatus(order.getStatus());
        vo.setRemark(order.getRemark());
        vo.setReviewDeadline(order.getReviewDeadline());
        vo.setCreatedTime(order.getCreatedTime());
        return vo;
    }
}
