package com.campus.common.log;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * WebFlux 版 traceId/tenantId MDC 过滤器（gateway 专用；Servlet 服务用 {@link MdcFilter}）。
 *
 * <p>traceId 优先取上游 X-Trace-Id，否则本地生成并向下游透传；
 * tenantId 取 X-School-Code，默认 CAMPUS-MAIN。
 */
public class TraceMdcWebFilter implements WebFilter, Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();
        String traceId = headers.getFirst(MdcFilter.HEADER_TRACE_ID);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        String tenantId = headers.getFirst(MdcFilter.HEADER_SCHOOL_CODE);
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = MdcFilter.DEFAULT_SCHOOL_CODE;
        }
        MDC.put(MdcFilter.TRACE_ID, traceId);
        MDC.put(MdcFilter.TENANT_ID, tenantId);
        final String tid = traceId;
        exchange.getResponse().getHeaders().set(MdcFilter.HEADER_TRACE_ID, tid);
        exchange.mutate().request(
                request.mutate().header(MdcFilter.HEADER_TRACE_ID, tid).build()).build();
        return chain.filter(exchange)
                .doFinally(sig -> {
                    MDC.remove(MdcFilter.TRACE_ID);
                    MDC.remove(MdcFilter.TENANT_ID);
                });
    }
}
