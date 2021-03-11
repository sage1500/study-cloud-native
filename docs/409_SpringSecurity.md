Spring Security
===

ここでは、Spring Security + WebFlux を利用して
OAuth & OpenID Connect による認証・認可の実装方法について紹介する。

なお、WebFlux を利用した場合の Spring Security の利用方法は、
WebFlux を利用しない場合とだいぶ異なることに注意。
（Web で Spring Security を調査する場合は、それが SpringMVC用に書かれているのか、WebFlux用に書かれているのか見極めが必要）

## 概要
OAuth の概要については以下参照。

http://terasolunaorg.github.io/guideline/5.6.1.RELEASE/ja/Security/OAuth.html


重要な部分をざっくりと説明する。

### ロール
OAuth 2.0 が定義するロールについて。
ここでいうロールとはユーザのロール（管理者や一般ユーザ）のことではなく、
プロトコルのシーケンス上に現れるライフライン（アクターやサーバ）のことを指す。
ロールには以下のものがある。

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

今回、Javaで実装するロール（ノード）は、上記のうち、
リソースサーバと、コンフィデンシャルクライアントの 2つである。

### アクセストークン

リソースオーナーに認可してもらった際に
認可サーバからクライアントに払いだされるトークン。
クライアントがリソースサーバにアクセスする際に、引き渡し、
リソースサーバ側に認可済みであることを示すためのもの。

### スコープ

ここでは触れない。

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

JWT(ジョット)。 JSON Web Token の略。
アクセストークンのフォーマットは任意となっている。
今回、アクセストークンのフォーマットに JWT を利用し、
アクセストークンに紐付く情報をアクセストークン自体の中に埋め込む方法で実装する。

参考) https://qiita.com/TakahikoKawasaki/items/970548727761f9e02bcd

## リソースサーバの実装

### 依存関係

OAuth2.0 のリソースサーバ用のスターターを依存関係に追加する。

```groovy
implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
```

### `application.properties`

お手軽な方法として、
`spring.security.oauth2.resourceserver.jwt.issuer-uri` 
だけを設定する方法を紹介する。

KeyCloak の場合、`/auth/realms/レルム名` をパスに設定する。
以下は、レルム名が demo の場合の設定例。

```
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:18080/auth/realms/demo
```

なお、AP起動時に、このURLにアクセスし、その他必要な情報を取得するという仕組みになっている。
そのため、AP起動時に、このURLへのアクセスできないと、APの起動が失敗する。
（APを起動する前に、KeyCloakが先に起動している必要がある）

### Java Config

認証の設定を行う。
大まかな実装方法は以下のとおり。

- `@EnableWebFluxSecurity`(このアノテーションは`@Configuration`を含んでいる) を付与した Configクラスで
    `SecurityWebFilterChain` のBean(Bean名は`springSecurityFilterChain`)を定義する。  
    ※TODO 要確認：Bean名は何でもよい？
- `SecurityWebFilterChain` の Bean生成時のポイント
    - `http.authorizeExchange()` で認可設定する。通常、全パスで認証を要求する設定でよい。（後で、Spring Boot Actuator を使用するときに少し改変する予定）
    - `http.oauth2ResourceServer().jwt()` でリソースサーバとしてJWTを使うように設定する。

例）
```java
@EnableWebFluxSecurity
public class SecurityConfig {
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        // 認可設定
        // @formatter:off
        http.authorizeExchange()
                .anyExchange().authenticated();
        // @formatter:on

        // リソースサーバ
        http.oauth2ResourceServer().jwt();
        return http.build();
    }
}
```

### アクセストークン（JWT）の入手方法

- ハンドラメソッドを任意に定義できる場合  
    ハンドラメソッドの引数に型 `@AuthenticationPrincipal Jwt` を指定することで、
    JWTを入手することができる。
    この JWT の `preferred_username`クレームからユーザ名を取得できる。

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
- Open API Generator を使って生成したハンドラメソッドの場合  
    生成されたハンドラメソッドには引数の `ServerWebExchange` が設定されている。
    `ServerWebExchange.getPrincipal()` を呼び出すと、
    `JwtAuthenticationToken` が取得でき、さらに `JwtAuthenticationToken.getToken()` にて JWT が取得できる。

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

Google などの有名どころを認証プロバイダとして利用する場合は、
Spring Security にあらかじめ組み込まれているため、設定が非常に少なくてよい。

KeyCloak の設定はデフォルトで組み込まれていないが、
ここではお手軽に KeyCloak を認証プロバイダとして登録する方法を紹介する。

認証プロバイダとして登録するお手軽な方法は、
`spring.security.oauth2.client.provider.プロバイダ名.issuer-uri` だけを設定すること。
KeyCloak の場合は、ここに `/auth/realms/レルム名` をパスに設定する。

以下は、プロバイダ名を keycloak とし、レルム名が demo の場合の設定例。

```
spring.security.oauth2.client.provider.keycloak.issuer-uri=http://localhost:18080/auth/realms/demo
```

なお、AP起動時に、このURLにアクセスし、その他必要な情報を取得するという仕組みになっている。
そのため、AP起動時に、このURLへのアクセスできないと、APの起動が失敗する。
（APを起動する前に、KeyCloakが先に起動している必要がある）

次に、
`spring.security.oauth2.client.registration.クライアント名.XXXX` の各種プロパティで
クライアントの情報を設定する。
`XXXX` で最低限必要なものは以下のとおり。

- `provider` : 上記で登録した認証プロバイダ名を設定
- `client-id` : KeyCloak上で登録しているクライアントID
- `client-secret` : KeyCloak上で登録しているクライアントシークレット
- `scope` : `openid` を設定する。重要。これを設定しないと、ログアウト時に OIDC(OpenID Connect)としてログアウト処理が行われない。

以下は、クライアント名が keycloak の場合の設定例。  
※プロバイダ名とクライアント名が同じなので、わかりにくくて申し訳ない・・・

```
spring.security.oauth2.client.registration.keycloak.provider=keycloak
spring.security.oauth2.client.registration.keycloak.client-id=demoapp
spring.security.oauth2.client.registration.keycloak.client-secret=08c33835-c18c-4dd7-a7df-aee3479d17c4
spring.security.oauth2.client.registration.keycloak.scope=openid
```

### WebClient

リソースサーバへのRESTの要求に対して、アクセストークンを載せる必要がある。
個々のRESTの送信処理にこの処理を埋め込まなくてもよいように、
WebClient にフィルター（`ExchangeFilterFunction`）を仕込むことで、これを実現する。
フィルタ自体は、Spring Security で提供されているため、実装自体は非常に簡単。

具体的には、
`ReactiveClientRegistrationRepository` と
`ServerOAuth2AuthorizedClientRepository` から
`ServerOAuth2AuthorizedClientExchangeFilterFunction` を生成し、
これを `WebClient` の `filter()` に登録する。

実装例）
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

なお、
`oauth.setDefaultClientRegistrationId("keycloak")`の引数には、
`application.properties` で設定した
`spring.security.oauth2.client.registration.XXXX` の `XXXX` の部分を設定する。
（上記のソースコードは `XXXX` の部分が `keycloak` の場合の例）

※実際は、`oauth.setDefaultClientRegistrationId("keycloak")` と、
`.baseUrl("http://localhost:8081/hello")` の引数の部分は `application.properties` で設定すべき。

### Java Config

認証の設定を行う。
大まかな実装方法は以下のとおり。

- `@EnableWebFluxSecurity`(このアノテーションは`@Configuration`を含んでいる) を付与した Configクラスで
    `SecurityWebFilterChain` のBean(Bean名は`springSecurityFilterChain`)を定義する。  
    ※TODO 要確認：Bean名は何でもよい？
- `SecurityWebFilterChain` の Bean生成時のポイント
    - `http.authorizeExchange()` で認可設定する。通常、トップ画面のように認証なしアクセス可能なパスを設定し、それ以外のパスは認証を要求する設定にする。（後で、Spring Boot Actuator を使用するときに少し改変する予定）
    - `http.oauth2Client` でOAuth2.0のクライアントとして動作するように設定する。
    - `http.logout()` でログアウト時に OIDC(OpenID Connect)としてログアウト処理をするように設定する。また、ログアウト後に遷移するパスを設定する。

実装例）
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
