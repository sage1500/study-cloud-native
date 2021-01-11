package com.example.hello.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebFluxSecurity
public class SecurityConfig {
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        // 認可設定
        // @formatter:off
        http.authorizeExchange()
                .pathMatchers("/manage/**").permitAll()
                .anyExchange().authenticated();
        // @formatter:on

        // リソースサーバ
        http.oauth2ResourceServer().jwt();
        return http.build();
    }
}
