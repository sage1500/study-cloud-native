package com.example.hello.app;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/hello")
@Slf4j
public class HelloController {
    @GetMapping("/")
    public String hello(@AuthenticationPrincipal Jwt jwt) {
        log.debug("[HELLO]hello jwt={}", jwt);
        return "Hello " + jwt.getClaimAsString("preferred_username");
    }
}
