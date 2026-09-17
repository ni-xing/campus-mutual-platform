package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.dto.PageRequest;
import com.campus.common.dto.PageResponse;
import com.campus.common.dto.UserContextHolder;
import com.campus.common.enums.GoodsCategory;
import com.campus.common.enums.GoodsStatus;
import com.campus.common.exception.BizException;
import com.campus.common.exception.ErrorCode;
import com.campus.trade.common.CacheKeys;
import com.campus.trade.dto.GoodsPublishRequest;
import com.campus.trade.dto.GoodsVO;
import com.campus.trade.dto.UserSnapshotVO;
import com.campus.trade.entity.Goods;
import com.campus.trade.feign.UserClient;
import com.campus.trade.mapper.GoodsMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品服务（specs/01 S-01~S-03）。
 *
 * <p>列表走 Redis 缓存（TTL 60s，AC-6：命中缓存响应显著低于首次）；
 * 浏览量 Redis 自增 + 定时任务回写 MySQL（S-03，削峰写压力）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsService {

    private static final Duration LIST_TTL = Duration.ofSeconds(60);

    private final GoodsMapper goodsMapper;
    private final StringRedisTemplate redis;
    private final UserClient userClient;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /** 发布商品（S-01；价格校验两位小数，MVP 直传图片 URL） */
    public Long publish(GoodsPublishRequest req) {
        Long userId = UserContextHolder.currentUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (req.getPrice().scale() > 2 || req.getPrice().compareTo(BigDecimal.valueOf(9999)) > 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "价格最多两位小数且不超过 9999");
        }
        Goods goods = new Goods();
        goods.setSellerId(userId);
        goods.setTitle(req.getTitle().trim());
        goods.setDescription(req.getDescription());
        goods.setCategory(req.getCategory());
        goods.setPrice(req.getPrice());
        goods.setImages(toJson(req.getImages()));
        goods.setStock(1);
        goods.setStatus(GoodsStatus.LISTED.name());
        goods.setAuditStatus("PASSED");
        goods.setViewCount(0);
        goods.setCreatedBy("user:" + userId);
        goodsMapper.insert(goods);
        evictListCache();
        return goods.getId();
    }

    /** 商品列表（S-02）：分页 + 类目筛选 + 标题模糊搜索，Redis 缓存 60s */
    public PageResponse<GoodsVO> list(Integer pageNo, Integer pageSize, String category, String keyword) {
        PageRequest pageRequest = new PageRequest(pageNo, pageSize);
        String cat = StringUtils.hasText(category) ? category : "all";
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : "none";
        String cacheKey = CacheKeys.GOODS_LIST + cat + ":" + kw + ":" + pageRequest.safePageNo() + ":" + pageRequest.safePageSize();

        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            return fromJson(cached);
        }

        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<Goods>()
                .eq(Goods::getStatus, GoodsStatus.LISTED.name())
                .orderByDesc(Goods::getCreatedTime);
        if (StringUtils.hasText(category)) {
            wrapper.eq(Goods::getCategory, category);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Goods::getTitle, keyword.trim());
        }
        Page<Goods> page = goodsMapper.selectPage(
                new Page<>(pageRequest.safePageNo(), pageRequest.safePageSize()), wrapper);
        PageResponse<GoodsVO> result = PageResponse.of(page.getTotal(), pageRequest.safePageSize(),
                page.getRecords().stream().map(g -> toVO(g, false)).toList());
        redis.opsForValue().set(cacheKey, toJson(result), LIST_TTL);
        return result;
    }

    /** 商品详情（S-03）：浏览量 Redis 自增（不阻塞主查询）；卖家信息 enrich（失败降级） */
    public GoodsVO detail(Long id) {
        Goods goods = requireGoods(id);
        redis.opsForValue().increment(CacheKeys.GOODS_VIEWS + id);
        return toVO(goods, true);
    }

    /** 我的商品（全部状态，卖家视角） */
    public PageResponse<GoodsVO> mine(Integer pageNo, Integer pageSize) {
        Long userId = UserContextHolder.currentUserId();
        PageRequest pageRequest = new PageRequest(pageNo, pageSize);
        Page<Goods> page = goodsMapper.selectPage(
                new Page<>(pageRequest.safePageNo(), pageRequest.safePageSize()),
                new LambdaQueryWrapper<Goods>()
                        .eq(Goods::getSellerId, userId)
                        .orderByDesc(Goods::getCreatedTime));
        return PageResponse.of(page.getTotal(), pageRequest.safePageSize(),
                page.getRecords().stream().map(g -> toVO(g, false)).toList());
    }

    /** 卖家下架（仅 LISTED 可下架） */
    public void offshelf(Long id) {
        Long userId = UserContextHolder.currentUserId();
        Goods goods = requireGoods(id);
        if (!goods.getSellerId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        if (goodsMapper.offshelfIfListed(id) != 1) {
            throw new BizException(ErrorCode.GOODS_UNAVAILABLE);
        }
        evictListCache();
    }

    Goods requireGoods(Long id) {
        Goods goods = goodsMapper.selectById(id);
        if (goods == null) {
            // spec §4：商品被删除时详情显示"已失效"，接口返回明确错误码
            throw new BizException(ErrorCode.PARAM_INVALID, "商品不存在或已失效");
        }
        return goods;
    }

    private GoodsVO toVO(Goods goods, boolean enrichSeller) {
        GoodsVO vo = new GoodsVO();
        vo.setId(goods.getId());
        vo.setSellerId(goods.getSellerId());
        vo.setTitle(goods.getTitle());
        vo.setDescription(goods.getDescription());
        vo.setCategory(goods.getCategory());
        vo.setPrice(goods.getPrice());
        vo.setImages(fromJsonImages(goods.getImages()));
        vo.setStatus(goods.getStatus());
        vo.setViewCount(goods.getViewCount());
        vo.setCreatedTime(goods.getCreatedTime());
        if (enrichSeller) {
            UserSnapshotVO seller = userClient.snapshotSafe(goods.getSellerId());
            if (seller != null) {
                vo.setSellerNickname(seller.nickname());
                vo.setSellerCreditScore(seller.creditScore());
            }
        }
        return vo;
    }

    /** 清列表缓存（发布/下架/成交状态变化后） */
    void evictListCache() {
        try (var cursor = redis.scan(ScanOptions.scanOptions().match(CacheKeys.GOODS_LIST_PREFIX).count(200).build())) {
            while (cursor.hasNext()) {
                redis.delete(cursor.next());
            }
        } catch (Exception e) {
            log.warn("evict goods list cache failed", e);
        }
    }

    private String toJson(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(images);
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "图片序列化失败");
        }
    }

    private List<String> fromJsonImages(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, String.class);
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String toJson(PageResponse<GoodsVO> page) {
        try {
            return objectMapper.writeValueAsString(page);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("goods list cache serialize failed", e);
        }
    }

    private PageResponse<GoodsVO> fromJson(String json) {
        try {
            JavaType type = objectMapper.getTypeFactory()
                    .constructParametricType(PageResponse.class, GoodsVO.class);
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            // 缓存损坏按未命中处理
            return new PageResponse<>(0, 0, new ArrayList<>());
        }
    }

    /** 类目合法性（列表筛选参数用，非法直接忽略） */
    static boolean isValidCategory(String category) {
        try {
            GoodsCategory.valueOf(category);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
