package com.example.frontweb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

//@Configuration
@Slf4j
public class LoggingFilterConfig {
    @Bean
    public WebFilter loggingFilter() {
        return (exchange, chain) -> chain.filter(exchange).transformDeferred(call -> Mono.fromRunnable(() -> {
            // Before
            var req = exchange.getRequest();
            log.info("BEFORE REQUEST: {} {} query={}", req.getMethod(), req.getURI(), req.getQueryParams());
            // log.info("★before request.cookies={}", req.getCookies());
            // log.info("★before request.headers={}", req.getHeaders());
        }).then(call).doOnSuccess(done -> {
            // after (success)
            var rsp = exchange.getResponse();
            log.info("AFTER SUCCESS: statusCode={}", rsp.getStatusCode());
        }).doOnError(throwable -> {
            // after (with error)
            log.info("AFTER ERROR: {}", throwable, throwable.getMessage());
        }));
    }
}
