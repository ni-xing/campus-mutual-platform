package com.campus.usercredit.controller;

import com.campus.common.dto.Result;
import com.campus.usercredit.dto.CreditDetailVO;
import com.campus.usercredit.service.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 信用分查询接口（specs/05 U-07）。 */
@RestController
@RequestMapping("/api/v1/credits")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;

    @GetMapping("/me")
    public Result<CreditDetailVO> me() {
        return Result.ok(creditService.myCredit());
    }
}
