package com.campus.common.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * traceId / tenantId MDC 过滤器（《系统设计》§8.2.2）。
 *
 * <p>traceId 优先取上游 X-Trace-Id（网关生成），否则本地生成；
 * tenantId 取 school_code，默认 CAMPUS-MAIN。
 */
public class MdcFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    public static final String TENANT_ID = "tenantId";
    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String HEADER_SCHOOL_CODE = "X-School-Code";
    public static final String DEFAULT_SCHOOL_CODE = "CAMPUS-MAIN";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(HEADER_TRACE_ID);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        String schoolCode = request.getHeader(HEADER_SCHOOL_CODE);
        if (schoolCode == null || schoolCode.isEmpty()) {
            schoolCode = DEFAULT_SCHOOL_CODE;
        }
        MDC.put(TRACE_ID, traceId);
        MDC.put(TENANT_ID, schoolCode);
        response.setHeader(HEADER_TRACE_ID, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
            MDC.remove(TENANT_ID);
        }
    }
}
