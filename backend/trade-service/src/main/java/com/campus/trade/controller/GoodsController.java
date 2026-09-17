package com.campus.trade.controller;

import com.campus.common.dto.PageResponse;
import com.campus.common.dto.Result;
import com.campus.trade.dto.GoodsPublishRequest;
import com.campus.trade.dto.GoodsVO;
import com.campus.trade.service.GoodsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品接口（specs/01 S-01~S-03）。登录态由网关 JWT 统一校验（00-全局约定 §6）。
 */
@RestController
@RequestMapping("/api/v1/goods")
@RequiredArgsConstructor
public class GoodsController {

    private final GoodsService goodsService;

    @PostMapping
    public Result<Long> publish(@Valid @RequestBody GoodsPublishRequest req) {
        return Result.ok(goodsService.publish(req));
    }

    @GetMapping
    public Result<PageResponse<GoodsVO>> list(@RequestParam(required = false) Integer pageNo,
                                              @RequestParam(required = false) Integer pageSize,
                                              @RequestParam(required = false) String category,
                                              @RequestParam(required = false) String keyword) {
        return Result.ok(goodsService.list(pageNo, pageSize, category, keyword));
    }

    @GetMapping("/mine")
    public Result<PageResponse<GoodsVO>> mine(@RequestParam(required = false) Integer pageNo,
                                              @RequestParam(required = false) Integer pageSize) {
        return Result.ok(goodsService.mine(pageNo, pageSize));
    }

    @GetMapping("/{id}")
    public Result<GoodsVO> detail(@PathVariable Long id) {
        return Result.ok(goodsService.detail(id));
    }

    @PostMapping("/{id}/offshelf")
    public Result<Void> offshelf(@PathVariable Long id) {
        goodsService.offshelf(id);
        return Result.ok();
    }
}
