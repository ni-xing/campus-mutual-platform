package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.dto.UserContextHolder;
import com.campus.common.exception.BizException;
import com.campus.common.exception.ErrorCode;
import com.campus.common.outbox.OutboxTemplate;
import com.campus.trade.dto.ReviewCreateRequest;
import com.campus.trade.dto.ReviewVO;
import com.campus.trade.entity.Review;
import com.campus.trade.entity.TradeOrder;
import com.campus.trade.feign.CreditClient;
import com.campus.trade.mapper.ReviewMapper;
import com.campus.trade.mapper.TradeOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 双向评价服务（specs/01 S-09）。
 *
 * <p>规则：订单 COMPLETED 后、评价窗口（完成 + 7 天）内，买/卖双方互评，
 * 每人每订单仅一次（uk_order_rater）；评星回流信用分（1:-5/2:-2/3:+1/4:+3/5:+5），
 * 回流失败仅记日志不回滚交易（最终一致，违约补偿由管理端处理）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final TradeOrderMapper orderMapper;
    private final CreditClient creditClient;
    private final OutboxTemplate outboxTemplate;

    @Transactional
    public ReviewVO create(Long orderId, ReviewCreateRequest req) {
        Long userId = UserContextHolder.currentUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        TradeOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "订单不存在");
        }
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        if (!"COMPLETED".equals(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATE_INVALID, "订单完成后才能评价");
        }
        if (order.getReviewDeadline() != null && LocalDateTime.now().isAfter(order.getReviewDeadline())) {
            throw new BizException(ErrorCode.REVIEW_WINDOW_CLOSED);
        }
        Long rateeId = order.getBuyerId().equals(userId) ? order.getSellerId() : order.getBuyerId();

        Review review = new Review();
        review.setOrderId(orderId);
        review.setOrderType("TRADE");
        review.setRaterId(userId);
        review.setRateeId(rateeId);
        review.setRating(req.getRating());
        review.setContent(req.getContent());
        review.setCreatedBy("user:" + userId);
        try {
            reviewMapper.insert(review);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.REVIEW_DUPLICATED);
        }

        // 评星 → 信用分回流（A020004 之外的最弱一致性：失败不回滚）
        adjustCreditSafe(orderId, userId, rateeId, req.getRating());
        outboxTemplate.publish("TRADE_ORDER_REVIEWED", order.getOrderNo(), rateeId,
                Map.of("orderNo", order.getOrderNo(), "rating", req.getRating(), "raterId", userId));
        return toVO(reviewMapper.selectById(review.getId()));
    }

    /** 订单的评价列表（双方互评展示） */
    public List<ReviewVO> listByOrder(Long orderId) {
        return reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                        .eq(Review::getOrderId, orderId).orderByAsc(Review::getCreatedTime))
                .stream().map(this::toVO).toList();
    }

    private void adjustCreditSafe(Long orderId, Long raterId, Long rateeId, int rating) {
        Integer delta = switch (rating) {
            case 1 -> -5;
            case 2 -> -2;
            case 3 -> 1;
            case 4 -> 3;
            default -> 5;
        };
        try {
            creditClient.adjust(Map.of(
                    "bizEventId", "REV-" + orderId + "-" + raterId,
                    "userId", rateeId,
                    "delta", delta,
                    "sourceType", "REVIEW"));
        } catch (Exception e) {
            log.warn("credit adjust failed (final consistent), orderId={} ratee={} delta={}",
                    orderId, rateeId, delta, e);
        }
    }

    private ReviewVO toVO(Review review) {
        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setOrderId(review.getOrderId());
        vo.setRaterId(review.getRaterId());
        vo.setRateeId(review.getRateeId());
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setCreatedTime(review.getCreatedTime());
        return vo;
    }
}
