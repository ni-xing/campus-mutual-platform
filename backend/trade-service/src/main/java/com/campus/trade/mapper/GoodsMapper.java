package com.campus.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.trade.entity.Goods;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface GoodsMapper extends BaseMapper<Goods> {

    /**
     * 条件占用商品（防超卖，AC-3）：仅 LISTED → SOLD，
     * 并发下单只允许一条 UPDATE 命中（行数判断），替代乐观锁重试循环。
     */
    @Update("UPDATE t_goods SET status = 'SOLD', version = version + 1, updated_time = NOW(3) "
            + "WHERE id = #{id} AND deleted = 0 AND status = 'LISTED'")
    int occupyIfListed(@Param("id") Long id);

    /** 取消退单回架：仅 SOLD → LISTED */
    @Update("UPDATE t_goods SET status = 'LISTED', version = version + 1, updated_time = NOW(3) "
            + "WHERE id = #{id} AND deleted = 0 AND status = 'SOLD'")
    int restoreToListed(@Param("id") Long id);

    /** 卖家下架：仅 LISTED → OFFSHELF */
    @Update("UPDATE t_goods SET status = 'OFFSHELF', version = version + 1, updated_time = NOW(3) "
            + "WHERE id = #{id} AND deleted = 0 AND status = 'LISTED'")
    int offshelfIfListed(@Param("id") Long id);

    /** 浏览量定时回写（Redis 增量冲账） */
    @Update("UPDATE t_goods SET view_count = view_count + #{delta} "
            + "WHERE id = #{id} AND deleted = 0")
    int incrViewCount(@Param("id") Long id, @Param("delta") long delta);
}
