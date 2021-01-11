package com.example.todo.common.filter;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class WebLoggingFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange).transformDeferred(call -> Mono.fromRunnable(() -> {
            // Before
            var req = exchange.getRequest();
            log.info("[WEB]REQUEST: {} {}", req.getMethod(), req.getURI());
            log.info("[WEB]REQUEST headers={}", req.getHeaders());
        }).then(call).doOnSuccess(done -> {
            // After (success)
            var rsp = exchange.getResponse();
            log.info("[WEB]SUCCESS: statusCode={}", rsp.getStatusCode());
        }).doOnError(throwable -> {
            // After (error)
            log.info("[WEB]ERROR: {}", throwable, throwable.getMessage());
        }));
    }
}
