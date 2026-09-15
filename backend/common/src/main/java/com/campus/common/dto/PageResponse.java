package com.campus.common.dto;

import java.util.List;

/** 分页响应（《系统设计》§3.2.1）。 */
public record PageResponse<T>(long total, long pages, List<T> records) {

    public static <T> PageResponse<T> of(long total, int pageSize, List<T> records) {
        long pages = pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new PageResponse<>(total, pages, records);
    }

    public static <T> PageResponse<T> empty() {
        return new PageResponse<>(0, 0, List.of());
    }
}
