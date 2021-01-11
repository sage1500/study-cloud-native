package com.example.frontweb.common;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class WebClientLoggingFilter implements ExchangeFilterFunction {
    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return next.exchange(request).transformDeferred(call -> Mono.fromRunnable(() -> {
            // Before
            log.info("[WEB-CLIENT]REQUEST: {} {}", request.method(), request.url());
        }).then(call).doOnSuccess(done -> {
            // after (success)
            log.info("[WEB-CLIENT]SUCCESS: statusCode={}", done.statusCode());
        }).doOnError(throwable -> {
            // after (with error)
            log.info("[WEB-CLIENT]ERROR: {}", throwable, throwable.getMessage());
        }));
    }
}
