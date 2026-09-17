package com.campus.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.trade.entity.BalanceAccount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 余额账户条件更新（资金安全核心）。
 *
 * <p>全部带余额/冻结额守卫条件，行数=0 即资金不足或账户异常，
 * 用"条件 UPDATE + 行数判断"替代乐观锁重试，避免 ABA 与自旋（《系统设计》§3.5.2 资金五问）。
 */
public interface BalanceAccountMapper extends BaseMapper<BalanceAccount> {

    /** 冻结：可用余额 → 冻结额（守卫 balance >= amount） */
    @Update("UPDATE t_balance_account SET balance = balance - #{amount}, frozen = frozen + #{amount}, "
            + "version = version + 1, updated_time = NOW(3) "
            + "WHERE user_id = #{userId} AND deleted = 0 AND balance >= #{amount}")
    int freeze(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /** 结算扣减（买家）：冻结额划走，同时累加交易积分 */
    @Update("UPDATE t_balance_account SET frozen = frozen - #{amount}, points = points + #{points}, "
            + "version = version + 1, updated_time = NOW(3) "
            + "WHERE user_id = #{userId} AND deleted = 0 AND frozen >= #{amount}")
    int settleDeduct(@Param("userId") Long userId, @Param("amount") BigDecimal amount, @Param("points") int points);

    /** 收款入账（卖家）：冻结外的净增，同时累加交易积分 */
    @Update("UPDATE t_balance_account SET balance = balance + #{amount}, points = points + #{points}, "
            + "version = version + 1, updated_time = NOW(3) "
            + "WHERE user_id = #{userId} AND deleted = 0")
    int income(@Param("userId") Long userId, @Param("amount") BigDecimal amount, @Param("points") int points);

    /** 解冻退款（取消）：冻结额 → 可用余额 */
    @Update("UPDATE t_balance_account SET frozen = frozen - #{amount}, balance = balance + #{amount}, "
            + "version = version + 1, updated_time = NOW(3) "
            + "WHERE user_id = #{userId} AND deleted = 0 AND frozen >= #{amount}")
    int unfreeze(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /** 模拟充值入账 */
    @Update("UPDATE t_balance_account SET balance = balance + #{amount}, "
            + "version = version + 1, updated_time = NOW(3) "
            + "WHERE user_id = #{userId} AND deleted = 0")
    int recharge(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
