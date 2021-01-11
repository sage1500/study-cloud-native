package com.example.frontweb.controller;

import java.net.URI;
import java.util.HashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;


@Configuration
@Slf4j
public class ReactTestController {

    public Mono<ServerResponse> test(ServerRequest req) {
        log.info("★react test");
        var attrs = new HashMap<String, Object>();
        attrs.put("hello", "Hello world.");
        return ServerResponse.ok().render("home", attrs);
    }

    public Mono<ServerResponse> test2(ServerRequest req) {
        log.info("★react test2");
        var attrs = new HashMap<String, Object>();
        attrs.put("hello", "Hello world2.");
        return ServerResponse.permanentRedirect(URI.create("/home")).build();
    }

    public Mono<ServerResponse> testPost(ServerRequest req) {
        log.info("★react testPost");
        return req.formData().flatMap(form -> {
            log.info("★form: {}", form);
            return ServerResponse.status(HttpStatus.MOVED_PERMANENTLY).location(URI.create("/home")).build();
            // temporaryRedirect() -> METHOD を変えずに再リクエスト
            // permanentRedirect() -> METHOD を変えずに再リクエスト
        });
    }

    @Bean
    public RouterFunction<ServerResponse> reactTestRouter() {
        return RouterFunctions.route()
            .GET("/test", this::test)
            .GET("/test2", this::test2)
            .POST("/testPost", this::testPost)
            .build();        
    }
}
