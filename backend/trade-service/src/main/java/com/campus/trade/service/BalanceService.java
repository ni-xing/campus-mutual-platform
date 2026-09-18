package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.dto.PageRequest;
import com.campus.common.dto.PageResponse;
import com.campus.common.dto.UserContextHolder;
import com.campus.common.enums.BalanceFlowType;
import com.campus.common.exception.BizException;
import com.campus.common.exception.ErrorCode;
import com.campus.trade.dto.BalanceVO;
import com.campus.trade.dto.FlowVO;
import com.campus.trade.entity.BalanceAccount;
import com.campus.trade.entity.BalanceFlow;
import com.campus.trade.mapper.BalanceAccountMapper;
import com.campus.trade.mapper.BalanceFlowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 余额服务（specs/01 S-07；方案 B 模拟支付的资金侧）。
 *
 * <p>资金安全三件套：条件 UPDATE 守卫（余额/冻结额不足即 0 行）+ t_balance_flow 幂等流水
 * （uk_biz_req）+ 事务一致性。冻结模型：下单冻结（FREEZE）→ 完成划款（DEDUCT/INCOME）
 * / 取消解冻（UNFREEZE）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceService {

    private final BalanceAccountMapper balanceAccountMapper;
    private final BalanceFlowMapper balanceFlowMapper;

    /** 查余额（含积分）；账户不存在则懒创建（注册即开户的 MVP 兜底口径） */
    public BalanceVO myBalance() {
        BalanceAccount account = getOrCreateAccount(UserContextHolder.currentUserId());
        BalanceVO vo = new BalanceVO();
        vo.setBalance(account.getBalance());
        vo.setFrozen(account.getFrozen());
        vo.setPoints(account.getPoints());
        return vo;
    }

    /** 模拟充值（方案 B：点击即到账，页面标注模拟环境；幂等由 @Idempotent + biz_req_no 双保险） */
    @Transactional
    public void recharge(Long userId, BigDecimal amount) {
        getOrCreateAccount(userId);
        if (balanceAccountMapper.recharge(userId, amount) != 1) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "充值入账失败");
        }
        BalanceAccount after = requireAccount(userId);
        insertFlow(userId, BalanceFlowType.RECHARGE, amount, 1,
                "RCG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24),
                "recharge", after);
    }

    /** 流水分页（本人） */
    public PageResponse<FlowVO> myFlows(PageRequest pageRequest) {
        Long userId = UserContextHolder.currentUserId();
        Page<BalanceFlow> page = balanceFlowMapper.selectPage(
                new Page<>(pageRequest.safePageNo(), pageRequest.safePageSize()),
                new LambdaQueryWrapper<BalanceFlow>()
                        .eq(BalanceFlow::getUserId, userId)
                        .orderByDesc(BalanceFlow::getCreatedTime));
        List<FlowVO> records = page.getRecords().stream().map(this::toFlowVO).toList();
        return PageResponse.of(page.getTotal(), pageRequest.safePageSize(), records);
    }

    /**
     * 余额账户懒创建。注册即开户由 M1 侧演进接入（user_db 与 trade_db 跨库，
     * MVP 在交易侧首次使用时兜底建户）。
     */
    BalanceAccount getOrCreateAccount(Long userId) {
        BalanceAccount account = balanceAccountMapper.selectOne(
                new LambdaQueryWrapper<BalanceAccount>().eq(BalanceAccount::getUserId, userId));
        if (account != null) {
            return account;
        }
        BalanceAccount created = new BalanceAccount();
        created.setUserId(userId);
        created.setBalance(BigDecimal.ZERO);
        created.setFrozen(BigDecimal.ZERO);
        created.setPoints(0);
        created.setCreatedBy("trade");
        try {
            balanceAccountMapper.insert(created);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 并发建户：uk_user 兜底，回落查询
        }
        return requireAccount(userId);
    }

    BalanceAccount requireAccount(Long userId) {
        BalanceAccount account = balanceAccountMapper.selectOne(
                new LambdaQueryWrapper<BalanceAccount>().eq(BalanceAccount::getUserId, userId));
        if (account == null) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "余额账户不存在");
        }
        return account;
    }

    /** 冻结买家余额（下单事务内调用；余额不足抛 A020002，AC-2） */
    void freezeForOrder(Long userId, BigDecimal amount, String orderNo) {
        // 买家可能从未充值：先兜底建户，否则 freeze 更新 0 行会被误报成"余额不足"
        getOrCreateAccount(userId);
        if (balanceAccountMapper.freeze(userId, amount) != 1) {
            throw new BizException(ErrorCode.BALANCE_NOT_ENOUGH);
        }
        insertFlow(userId, BalanceFlowType.FREEZE, amount, 0, "FRZ-" + orderNo, orderNo, requireAccount(userId));
    }

    /** 完成结算：买家冻结划走 + 卖家入账（各得 amount 1% 积分，向下取整；AC-1/S-07） */
    void settle(Long buyerId, Long sellerId, BigDecimal amount, String orderNo) {
        int buyerPoints = amount.multiply(BigDecimal.valueOf(0.01)).setScale(0, java.math.RoundingMode.FLOOR).intValue();
        if (balanceAccountMapper.settleDeduct(buyerId, amount) != 1) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "买家结算失败（冻结额异常）");
        }
        insertFlow(buyerId, BalanceFlowType.DEDUCT, amount, 0, "DED-" + orderNo, orderNo, requireAccount(buyerId));
        // 卖家可能从未充值/查余额，账户尚未懒创建——收款前必须兜底建户，
        // 否则 income 更新 0 行 → "卖家入账失败"，订单永远无法完成（曾为线上级缺陷）
        getOrCreateAccount(sellerId);
        if (balanceAccountMapper.income(sellerId, amount, buyerPoints) != 1) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "卖家入账失败");
        }
        insertFlow(sellerId, BalanceFlowType.INCOME, amount, 1, "INC-" + orderNo, orderNo, requireAccount(sellerId));
        // 积分独立累加（1% 向下取整；金额 <1 元时积分为 0 属预期）
        if (buyerPoints > 0) {
            balanceAccountMapper.addPoints(buyerId, buyerPoints);
        }
    }

    /** 取消解冻：冻结额原路退回买家（AC-4） */
    void unfreezeForOrder(Long userId, BigDecimal amount, String orderNo) {
        if (balanceAccountMapper.unfreeze(userId, amount) != 1) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "解冻失败（冻结额异常）");
        }
        insertFlow(userId, BalanceFlowType.UNFREEZE, amount, 1, "UNF-" + orderNo, orderNo, requireAccount(userId));
    }

    private void insertFlow(Long userId, BalanceFlowType type, BigDecimal amount, int direction,
                            String bizReqNo, String bizRef, BalanceAccount after) {
        BalanceFlow flow = new BalanceFlow();
        flow.setUserId(userId);
        flow.setFlowType(type.name());
        flow.setAmount(amount);
        flow.setDirection(direction);
        flow.setBizReqNo(bizReqNo);
        flow.setBizRef(bizRef);
        flow.setBalanceAfter(after.getBalance());
        flow.setFrozenAfter(after.getFrozen());
        flow.setCreatedBy("trade");
        try {
            balanceFlowMapper.insert(flow);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 幂等命中（uk_biz_req）：资金操作重复调用时安全返回
            log.info("balance flow idempotent hit userId={} bizReqNo={}", userId, bizReqNo);
        }
        flow.setCreatedTime(LocalDateTime.now());
    }

    private FlowVO toFlowVO(BalanceFlow flow) {
        FlowVO vo = new FlowVO();
        vo.setId(flow.getId());
        vo.setFlowType(flow.getFlowType());
        vo.setAmount(flow.getAmount());
        vo.setDirection(flow.getDirection());
        vo.setBizRef(flow.getBizRef());
        vo.setBalanceAfter(flow.getBalanceAfter());
        vo.setFrozenAfter(flow.getFrozenAfter());
        vo.setCreatedTime(flow.getCreatedTime());
        return vo;
    }
}
