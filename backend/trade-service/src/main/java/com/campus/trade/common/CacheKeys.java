package com.campus.trade.common;

/**
 * M2 Redis 键约定（specs/01 S-02/S-03）。
 */
public final class CacheKeys {

    /** 商品列表缓存：campus:trade:goods:list:{category|all}:{kw|none}:{page}:{size}，TTL 60s */
    public static final String GOODS_LIST = "campus:trade:goods:list:";

    /** 商品列表缓存统一前缀（发布/状态变更后按前缀清缓存） */
    public static final String GOODS_LIST_PREFIX = "campus:trade:goods:list:*";

    /** 浏览量计数：campus:trade:goods:views:{goodsId}，定时任务回写 MySQL */
    public static final String GOODS_VIEWS = "campus:trade:goods:views:";

    /** 浏览量计数扫描前缀 */
    public static final String GOODS_VIEWS_PREFIX = "campus:trade:goods:views:*";

    private CacheKeys() {
    }
}
