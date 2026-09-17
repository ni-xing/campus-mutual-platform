package com.campus.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.trade.entity.TradeOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface TradeOrderMapper extends BaseMapper<TradeOrder> {

    /**
     * 状态机条件转移：仅允许 from → to 的合法流转，0 行即非法流转/并发冲突
     * （specs/01 S-06 / AC-5。乐观锁只防并发写，不防非法方向，必须显式守卫 status）。
     */
    @Update("UPDATE t_trade_order SET status = #{to}, "
            + "review_deadline = COALESCE(#{deadline}, review_deadline), "
            + "version = version + 1, updated_by = #{updatedBy}, updated_time = NOW(3) "
            + "WHERE id = #{id} AND status = #{from} AND deleted = 0")
    int transition(@Param("id") Long id,
                   @Param("from") String from,
                   @Param("to") String to,
                   @Param("deadline") LocalDateTime deadline,
                   @Param("updatedBy") String updatedBy);
}
