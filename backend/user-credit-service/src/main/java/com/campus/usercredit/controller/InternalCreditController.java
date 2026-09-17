package com.campus.usercredit.controller;

import com.campus.common.dto.Result;
import com.campus.usercredit.dto.CreditAdjustRequest;
import com.campus.usercredit.dto.CreditSnapshotVO;
import com.campus.usercredit.service.CreditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部接口（specs/05 U-07）：供 M2/M4 服务间调用，网关侧 /internal/** 404，不暴露公网。
 */
@RestController
@RequestMapping("/internal/credit")
@RequiredArgsConstructor
public class InternalCreditController {

    private final CreditService creditService;

    @GetMapping("/{userId}")
    public Result<CreditSnapshotVO> snapshot(@PathVariable Long userId) {
        return Result.ok(creditService.snapshot(userId));
    }

    @PostMapping("/adjust")
    public Result<Void> adjust(@Valid @RequestBody CreditAdjustRequest req) {
        creditService.adjust(req);
        return Result.ok();
    }
}
