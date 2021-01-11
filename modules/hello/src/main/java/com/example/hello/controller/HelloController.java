package com.example.hello.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/hello")
@Slf4j
public class HelloController {
    @GetMapping
    public Mono<String> hello(@AuthenticationPrincipal Jwt jwt) {
        log.info("★hello jwt={}", jwt);
        return Mono.just("Hello " + jwt.getClaimAsString("preferred_username"));
    }
}
