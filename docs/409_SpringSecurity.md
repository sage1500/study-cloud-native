Spring Security
===

TODO 書きかけ


ここでは、Spring Security + WebFlux を利用して
OAuth & OpenID Connect による認証・認可の実装方法について紹介する。

なお、WebFlux を利用した場合の Spring Security の利用方法は、
WebFlux を利用しない場合とだいぶ異なることに注意。

## 概要
OAuth の概要については以下参照。

http://terasolunaorg.github.io/guideline/5.6.1.RELEASE/ja/Security/OAuth.html


重要な部分をざっくりと説明する。

### ロール
OAuth 2.0 が定義するロールについて。
ここでいうロールとはユーザのロール（管理者や一般ユーザ）のことではなく、
プロトコルのシーケンス上に現れるライフライン（アクターやサーバ）のことを指す。

- リソースオーナ  
    人のことだと思っておけばよい。
- リソースサーバ  
    REST のサーバ側と思っておけばよい。
- クライアント  
    REST のクライアント側と思っておけばよい。
    クライアントのバリエーションとして以下のものがある。
    - コンフィデンシャル（クライアント）  
        Webアプリケーションから REST を呼び出すケース。
    - パブリック（クライアント）  
        ブラウザ上で動作する JavaScriptアプリケーションや、ネイティブアプリケーションから REST を呼び出すケース。
- 認可サーバ  
    KeyCloak のことと思っておけばよい。

今回、Javaで実装するのは、上記のうち、
リソースサーバと、コンフィデンシャルクライアントの 2つ。

### アクセストークン

リソースオーナーに認可してもらった際に
認可サーバからクライアントに払いだされるトークン。
クライアントがリソースサーバにアクセスする際に、引き渡し、
リソースサーバ側に認可済みであることを示すためのもの。

### スコープ

TODO

### 認可グラント

とりあえず、グラントタイプとして、認可コードグラントを利用するということを覚えておけばよい。

### 認証？認可？

認証は、人（ユーザおよび、リソースオーナー）を認証すること。
認可は、Webアプリケーションと、OAuth で意味が違うので、注意が必要。

- Webアプリケーション  
    ユーザ（人）毎に管理者や一般ユーザなどのロールを割り当て、ロールごとにアクセス制限すること。
- OAuth  
    リソースオーナー（人）がクライアントに対して、リソースサーバへのアクセスを許可すること。
    スマホを使っているとカメラを利用してよいか、など許可を求める画面が表示されることがあるが、それをイメージするとよい。
    なお、OAuthを使ったアプリケーションでもリソースを利用してよいか許可を求める画面を表示させることができるが、表示させないケースが多い。そのため、自分で認可したという実感がないため、OAuth での認可はイメージしずらい。

OAuth における認可は、リソースオーナーが認可するため、
まず、リソースオーナーが本人かどうか確認するための認証が必要になる。
そのため、認可サーバは認証機能も持っていることが多い。
今回利用する KeyCloak も認証機能を持っている。

### JWT

JWT(ジョット)。
アクセストークンのフォーマットは任意。
今回は、アクセストークンに JWT を利用する。

## リソースサーバの実装

```groovy
implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
```

```
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:18080/auth/realms/demo
```

```java
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
```

```java
@GetMapping("/")
public String hello(@AuthenticationPrincipal Jwt jwt) {
    log.debug("[HELLO]hello jwt={}", jwt);
    return "Hello " + jwt.getClaimAsString("preferred_username");
}
```

※FQCNに注意
```java
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
```

```java
@Override
public Mono<ResponseEntity<TodoResource>> createTodo(@Valid Mono<TodoResource> todoResource,
        ServerWebExchange exchange) {
    // @formatter:off
    return Mono.zip(
            exchange.getPrincipal().cast(JwtAuthenticationToken.class),
            todoResource)
        .flatMap(t -> {
            var jwt = t.getT1().getToken();
            var rsrc = t.getT2();
            String todoId = null;
            return todoService.save(toTodo(rsrc, todoId, jwt));
        })
        .doOnNext(this::notifyCreateTodo)
        .map(todo -> ResponseEntity.ok(toTodoResource(todo)));
    // @formatter:on
}
```

※FQCNに注意。間違えると実行時に `ClassCastException` が発生する。
```java
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
```

## コンフィデンシャルクライアントの実装

### 依存関係

OAuth2.0 のクライアント用のスターターを依存関係に追加する。

```groovy
implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
```

### `application.properties`

TODO

```
spring.security.oauth2.client.registration.keycloak.provider=keycloak
spring.security.oauth2.client.registration.keycloak.client-id=demoapp
spring.security.oauth2.client.registration.keycloak.client-secret=08c33835-c18c-4dd7-a7df-aee3479d17c4
spring.security.oauth2.client.registration.keycloak.scope=openid
spring.security.oauth2.client.provider.keycloak.issuer-uri=http://localhost:18080/auth/realms/demo
```

### WebClient

リソースサーバへのRESTの要求にアクセストークンを載せるための設定を追加する。
具体的には、
`ReactiveClientRegistrationRepository` と
`ServerOAuth2AuthorizedClientRepository` から
`ServerOAuth2AuthorizedClientExchangeFilterFunction` を生成し、
`WebClient` の `filter()` に登録する。

```java
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
```

`oauth.setDefaultClientRegistrationId("keycloak")`の引数には、
`application.properties` で設定した
`spring.security.oauth2.client.registration.XXXX` の `XXXX` の部分を設定する。

※実際は、`oauth.setDefaultClientRegistrationId("keycloak")` と、
`.baseUrl("http://localhost:8081/hello")` の引数の部分は `application.properties` で設定すべき。

### Java Config

TODO

```java
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
            ReactiveClientRegistrationRepository clientRegistrationRepository) {
        // CSRF対策
        // ※CSRFトークンを Cookieに保持
        http.csrf().csrfTokenRepository(new CookieServerCsrfTokenRepository());

        // 認可設定
        // @formatter:off
        http.authorizeExchange()
                .pathMatchers("/").permitAll()
                .pathMatchers("/manage/**").permitAll()
                .anyExchange().authenticated();
        // @formatter:on

        // OAuth2 Client
        http.oauth2Client();

        // OAuth2 ログイン
        http.oauth2Login();

        // ログアウト
        http.logout(logout -> {
            var logoutSuccessHandler = new OidcClientInitiatedServerLogoutSuccessHandler(clientRegistrationRepository);
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
```

### ControllerAdvice

クラス名は任意だが、以下の ControllerAdvice を用意する必要がある。

※これがないと、Thymeleaf が POST時の FORM に対して CSRF のトークンを埋め込んでくれない。
結果として、すべての POST がサーバ側のCSRF対策に引っ掛かり処理されない。

```java
@ControllerAdvice
public class SecurityControllerAdvice {
    @ModelAttribute
    public Mono<CsrfToken> csrfToken(ServerWebExchange exchange) {
        Mono<CsrfToken> csrfToken = exchange.getAttribute(CsrfToken.class.getName());
        return csrfToken.doOnSuccess(
                token -> exchange.getAttributes().put(CsrfRequestDataValueProcessor.DEFAULT_CSRF_ATTR_NAME, token));
    }
}
```

※FQCNに注意。間違えると実行時に `NullPointerException` が発生する。
```java
import org.springframework.security.web.server.csrf.CsrfToken;
```


### ログアウトボタンの実装
ログアウトは、`@{/logout}` に対して、POSTすることで実現する。

```html
<form th:action="@{/logout}" method="post">
    <button th:text="#{common.logout}">Logout</button>
</form>
```
