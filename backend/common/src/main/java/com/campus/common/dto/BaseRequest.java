package com.campus.common.dto;

import lombok.Data;

/** 请求基类：携带 traceId 透传位（《系统设计》§3.2.1）。 */
@Data
public class BaseRequest {
    private String traceId;
}
