package com.campus.common.dto;

/**
 * 分页请求：pageSize 上限 100（《系统设计》§4.3.3 分页上限，防爬）。
 */
public record PageRequest(Integer pageNo, Integer pageSize) {

    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_SIZE = 10;

    public int safePageNo() {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    public int safePageSize() {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    public long offset() {
        return (long) (safePageNo() - 1) * safePageSize();
    }
}
