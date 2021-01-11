package com.example.frontweb.config;

import com.example.api.frontweb.client.api.TodosApi;
import com.example.api.frontweb.client.invoker.ApiClient;
import com.example.frontweb.common.WebClientLoggingFilter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class BackConfig {
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

    @Bean
    public WebClient webClientForTodo(ReactiveClientRegistrationRepository clientRegistrations,
            ServerOAuth2AuthorizedClientRepository authorizedClients, WebClientLoggingFilter loggingFilter) {
        var oauth = new ServerOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients);

        oauth.setDefaultOAuth2AuthorizedClient(true);
        oauth.setDefaultClientRegistrationId("keycloak");

        ExchangeStrategies strategies = createExchangeStrategiesForTodosApi();

        // @formatter:off
        return WebClient.builder()
                .baseUrl("http://localhost:8082/todos")
                .filter(oauth)
                .filter(loggingFilter)
                .exchangeStrategies(strategies)
                .build();
        // @formatter:on
    }

    @Bean
    public WebClient webClientForTodo2(ReactiveClientRegistrationRepository clientRegistrations,
            ServerOAuth2AuthorizedClientRepository authorizedClients) {
        var oauth = new ServerOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients);

        oauth.setDefaultOAuth2AuthorizedClient(true);
        oauth.setDefaultClientRegistrationId("keycloak");

        // @formatter:off
        return WebClient.builder()
                .baseUrl("http://localhost:8082/todos")
                .filter(oauth)
                .build();
        // @formatter:on
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }

    @Bean
    public TodosApi todosApi(@Qualifier("webClientForTodo") WebClient webClient) {
        var clientProto = new ApiClient();
        ApiClient apiClient = new ApiClient(webClient, null, clientProto.getDateFormat());
        apiClient.setBasePath("http://localhost:8082");
        var api = new TodosApi(apiClient);
        return api;
    }

    private ExchangeStrategies createExchangeStrategiesForTodosApi() {
        // 自動生成コードからコピー
        var clientProto = new ApiClient();
        ObjectMapper mapper = new ObjectMapper();
        mapper.setDateFormat(clientProto.getDateFormat());
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonNullableModule jnm = new JsonNullableModule();
        mapper.registerModule(jnm);

        return ExchangeStrategies.builder().codecs(clientDefaultCodecsConfigurer -> {
            clientDefaultCodecsConfigurer.defaultCodecs()
                    .jackson2JsonEncoder(new Jackson2JsonEncoder(mapper, MediaType.APPLICATION_JSON));
            clientDefaultCodecsConfigurer.defaultCodecs()
                    .jackson2JsonDecoder(new Jackson2JsonDecoder(mapper, MediaType.APPLICATION_JSON));
        }).build();
    }
}
