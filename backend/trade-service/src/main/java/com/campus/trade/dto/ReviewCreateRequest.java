package com.campus.trade.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 评价请求（specs/01 S-09：1-5 星 + 评语 ≤200 字，每人每订单仅一次）。 */
@Data
public class ReviewCreateRequest {

    @Min(1)
    @Max(5)
    private int rating;

    @Size(max = 200)
    private String content;
}
