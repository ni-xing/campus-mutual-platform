package com.campus.trade.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品发布请求（specs/01 S-01）：标题 ≤50、描述 ≤500、价格 (0,9999] 两位小数、图片 ≤9。
 */
@Data
public class GoodsPublishRequest {

    @NotBlank
    @Size(max = 50)
    private String title;

    @Size(max = 500)
    private String description;

    /** 类目：BOOK/DIGITAL/DAILY/SPORT/OTHER */
    @NotBlank
    @Pattern(regexp = "BOOK|DIGITAL|DAILY|SPORT|OTHER")
    private String category;

    @NotNull
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;

    /** 图片 URL，≤9 张（MVP 直传 URL，文件上传后续接） */
    @Size(max = 9, message = "图片最多 9 张")
    private List<String> images;
}
