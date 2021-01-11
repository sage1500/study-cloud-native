package com.example.frontweb.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
            ReactiveClientRegistrationRepository clientRegistrationRepository) {
        // TODO WebFlux で CSRF がまともに動かない？
        http.csrf().disable();

        // 認可設定
        http.authorizeExchange()
                .pathMatchers("/").permitAll()
                // .pathMatchers("/logout").permitAll() TODO logout は書かなくてもOK？
                .anyExchange().authenticated();

        // OAuth2 Client
        // TODO これあるべき？不要？
        http.oauth2Client();

        // OAuth2 ログイン
        http.oauth2Login();

        // ログアウト
        http.logout(logout -> {
            var logoutSuccessHandler = new MyOidcClientInitiatedServerLogoutSuccessHandler(
                    clientRegistrationRepository);
            logoutSuccessHandler.setLogoutSuccessUrl(URI.create("/"));
            logoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/");

            // OIDC上もログアウトするように
            logout.logoutSuccessHandler(logoutSuccessHandler);

            // 参考）以下、デフォルトの設定
            // .logoutUr: POST /logout
            // .logoutHandler: SecurityContextServerLogoutHandler
            // .logoutSuccessHandler: RedirectServerLogoutSuccessHandler
        });

        return http.build();
    }

}
