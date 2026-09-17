package com.campus.usercredit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.dto.UserContextHolder;
import com.campus.common.exception.BizException;
import com.campus.common.exception.ErrorCode;
import com.campus.usercredit.dto.CreditAdjustRequest;
import com.campus.usercredit.dto.CreditDetailVO;
import com.campus.usercredit.dto.CreditRecordVO;
import com.campus.usercredit.dto.CreditSnapshotVO;
import com.campus.usercredit.entity.CreditRecord;
import com.campus.usercredit.entity.UserAccount;
import com.campus.usercredit.mapper.CreditRecordMapper;
import com.campus.usercredit.mapper.UserAccountMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 信用分服务（specs/05 U-07；《系统设计》§3.2.M1.5）。
 *
 * <p>回流机制：M2/M4 评价、履约事件经内部接口回流加分——
 * bizEventId 唯一索引兜底恰好一次生效，分数钳制 [0,200]，乐观锁防并发覆盖。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditService {

    private static final int SCORE_MIN = 0;
    private static final int SCORE_MAX = 200;

    private final CreditRecordMapper creditRecordMapper;
    private final UserAccountMapper userAccountMapper;

    /** 本人信用分与明细（/credits/me） */
    public CreditDetailVO myCredit() {
        Long userId = UserContextHolder.currentUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        List<CreditRecordVO> records = creditRecordMapper.selectList(
                        new LambdaQueryWrapper<CreditRecord>()
                                .eq(CreditRecord::getUserId, userId)
                                .orderByDesc(CreditRecord::getCreatedTime)
                                .last("LIMIT 50"))
                .stream().map(this::toVO).toList();
        CreditDetailVO vo = new CreditDetailVO();
        vo.setUserId(userId);
        vo.setCreditScore(user.getCreditScore());
        vo.setRecords(records);
        return vo;
    }

    /** 内部信用快照（/internal/credit/{userId}，供 M2 检索排序 / M4 接单优先级） */
    public CreditSnapshotVO snapshot(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "用户不存在");
        }
        return new CreditSnapshotVO(userId, user.getCreditScore());
    }

    /**
     * 信用分回流（/internal/credit/adjust，AC-11）：
     * 幂等查重 → 事务内 INSERT 明细 + 乐观锁 UPDATE 分数 → 分数钳制 [0,200]。
     */
    @Transactional
    public void adjust(CreditAdjustRequest req) {
        // Step 1 幂等校验：bizEventId 已存在直接返回成功（防重复加分）
        Long existed = creditRecordMapper.selectCount(
                new LambdaQueryWrapper<CreditRecord>().eq(CreditRecord::getBizEventId, req.getBizEventId()));
        if (existed != null && existed > 0) {
            log.info("CREDIT_ADJUST idempotent hit bizEventId={}", req.getBizEventId());
            return;
        }

        // Step 2 本地事务：明细 + 分数（乐观锁；version 冲突由调用方重试）
        UserAccount user = userAccountMapper.selectById(req.getUserId());
        if (user == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "用户不存在");
        }
        int after = Math.max(SCORE_MIN, Math.min(SCORE_MAX, user.getCreditScore() + req.getDelta()));

        CreditRecord record = new CreditRecord();
        record.setSchoolCode(user.getSchoolCode());
        record.setUserId(req.getUserId());
        record.setDelta(req.getDelta());
        record.setSourceType(req.getSourceType());
        record.setScoreAfter(after);
        record.setBizEventId(req.getBizEventId());
        record.setCreatedBy("internal");
        try {
            creditRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 并发重复：唯一索引兜底，幂等返回成功
            log.info("CREDIT_ADJUST duplicate bizEventId={} (uk fallback)", req.getBizEventId());
            return;
        }

        user.setCreditScore(after);
        int rows = userAccountMapper.updateById(user);
        if (rows == 0) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "信用分更新冲突，请重试");
        }
        log.info("CREDIT_ADJUST userId={} delta={} after={} bizEventId={}",
                req.getUserId(), req.getDelta(), after, req.getBizEventId());
    }

    private CreditRecordVO toVO(CreditRecord record) {
        CreditRecordVO vo = new CreditRecordVO();
        vo.setDelta(record.getDelta());
        vo.setSourceType(record.getSourceType());
        vo.setScoreAfter(record.getScoreAfter());
        vo.setBizEventId(record.getBizEventId());
        vo.setCreatedAt(record.getCreatedTime());
        return vo;
    }
}
