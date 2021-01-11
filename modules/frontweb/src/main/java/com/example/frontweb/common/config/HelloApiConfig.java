package com.example.frontweb.common.config;

import com.example.frontweb.common.filter.WebClientLoggingFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class HelloApiConfig {
    @Bean
    public WebClient webClientForHello(ReactiveClientRegistrationRepository clientRegistrations,
            ServerOAuth2AuthorizedClientRepository authorizedClients, WebClientLoggingFilter loggingFilter) {
        var oauth = new ServerOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients);

        oauth.setDefaultOAuth2AuthorizedClient(true);
        oauth.setDefaultClientRegistrationId("keycloak");

        // @formatter:off
        return WebClient.builder()
                .baseUrl("http://localhost:8081/hello")
                .filter(oauth)
                .filter(loggingFilter)
                .build();
        // @formatter:on
    }
}
