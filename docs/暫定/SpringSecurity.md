Spring Security
===

認可
---

- 認証・認可は Javaコードは書かなくてもできる（はず）
- BFF のパターンで、BFFをクライアント、APIサーバをリソースサーバとして扱うことに意味はあるのか？
  - BFF がリソースサーバなのでは？

### 参考文献

- WebFlux での OAuth
  - https://spring.pleiades.io/spring-security/site/docs/current/reference/html5/#reactive-applications


### クライアント実装

#### 依存関係

```groovy
implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
```

この依存関係を書くだけで、URLアクセス時にログイン画面が表示されるようになる。
ただ、特に認証情報を用意していないので、その先に進めない。

#### プロパティ(google)

Google の最小設定。

```
spring.security.oauth2.client.registration.google.client-id=<ClientID>
spring.security.oauth2.client.registration.google.client-secret=<ClientSecret>
```

なお、上記 `client-id` と `client-secret` は、`https://console.developers.google.com` で発行する必要あり。

#### プロパティ(KeyCloak)

KeyCloak の最小設定。

```
### registration
spring.security.oauth2.client.registration.keycloak.provider=keycloak
spring.security.oauth2.client.registration.keycloak.client-id=<ClientID>
spring.security.oauth2.client.registration.keycloak.client-secret=<ClientSecret>
spring.security.oauth2.client.registration.keycloak.scope=openid
spring.security.oauth2.client.provider.keycloak.issuer-uri=http://<KeyCloakHost>:<KeyCloakPort>/auth/realms/<レルム名>
```

- `.client-secret` の部分は、keycloakのクライアント設定でアクセスタイプが `public` の場合は必須ではない。
- `.scope` の設定は重要。これがなくてもログインできるが、以下の問題がある。
  - プリンシパルの型が OidcUser にならない。
  - ログアウト時に KeyCloak へのログアウトしない。
- `issuer-uri` を使うことで定義量を減らせる。が、起動時にアクセスするため、先に KeyCloak が起動していないとAPの起動に失敗する。



ちなみに、Keycloak Spring Bootアダプターというのもあるらしい。
```
implementation 'org.keycloak:keycloak-spring-boot-starter:8.0.1'
```
が、別に利用するほどのことはなさそう（？）

#### IDプロバイダへのクライアント登録

- リダイレクトURI
  - 認証後にリダイレクトするクライアント側のURL（認可コード送信先のURLとなる（はず））の検証用の設定
  - keycloak
    - 項目：「有効なリダイレクトURI」
    - 次のように最後の部分だけ "/*" とワイルドカードで表記できる。  
      `http://localhost:8080/login/oauth2/code/*`
  - google 
    - 項目：「承認済みのリダイレクト URI」
      `http://localhost:8080/login/oauth2/code/google`

#### コントローラー

- 引数: Authentication
- 引数: OAuth2AuthenticationToken
  - Authentication.getAuthorities() 以外のメソッドは Object を返す。
  - Authentication.getAuthorities() 以外のメソッドが返すオブジェクトを扱う場合は、
    実装クラス(OAuth2AuthenticationTokenなど)を引数に指定する。
  - OAuth2AuthenticationToken の場合は、
    `authenticationToken.getPrincipal().getAttribute("preferred_username")` でユーザ名がとれる。

- 引数: @AuthenticationPrincipal OAuth2AuthenticatedPrincipal
- 引数: @AuthenticationPrincipal OAuth2User
- 引数: @AuthenticationPrincipal OidcUser
  - Principal の部分だけ取得できる。
  - OidcUser.getPreferredUsername() でユーザ名が取得できる。
  - OpenId Connect を使っているなら、OidcUser を使った方がよい。 

- 引数: @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient
  - アクセストークンやリフレッシュトークンが取得できる。
    - アクセストークンをAPIサーバに連携する際に必要になる。  
      ※連携する処理を WebClient のフィルタで仕込んでいる場合は必須ではない。
  - 例:  
    ```Java
    @RestController
    @RequestMapping("/hello")
    public class HelloController {
        @GetMapping()
        public String hello(@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient) {
            var accessToken = authorizedClient.getAccessToken().getTokenValue();
            return "hello " + new Date() + " accessToken: " + accessToken;
        }
    }
    ```
    ↑の`("keycloak")`の部分は registrationId の指定であるが、なくても動く。

#### Java Config
以下、`.pathMatchers("/").permitAll()` の部分をユーザを認可する設定に置き換える。

```Java
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
            ReactiveClientRegistrationRepository clientRegistrationRepository) {
        // CSRF対策
        // ※CSRFトークンを Cookieに保持
        http.csrf().csrfTokenRepository(new CookieServerCsrfTokenRepository());

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
```

#### HTML

- ログイン  
  ログイン後のトップページ用の URL を呼び出すようにボタンやリンクを用意する良い。
  ```html
  <form th:action="@{/home}" method="get">
      <button type="submit">ログイン</button>
  </form>
  ```

- ログアウト  
  ログアウトは Java Config で設定したログアウト用の URL(デフォルトは `/logout`) を呼び出すように
  ボタンやリンクを用意する。
  ```html
      <form th:action="@{/logout}" method="post">
          <button type="submit">ログアウト</button>
      </form>
  ```



WebClient のBean定義。
サービスごとに用意することで、利用する側の記載が楽になる。
不用意に異なるサービス用の WebClient を DI しないように
その他サービス用の WebClient の Bean定義も用意した方がよい（だろう）。

```Java
    @Bean
    public WebClient webClientForServiceA(ReactiveClientRegistrationRepository clientRegistrations,
            ServerOAuth2AuthorizedClientRepository authorizedClients) {
        var oauth = new ServerOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients);

        // (optional) explicitly opt into using the oauth2Login to provide an access
        // token implicitly
        oauth.setDefaultOAuth2AuthorizedClient(true);

        // (optional) set a default ClientRegistration.registrationId
        oauth.setDefaultClientRegistrationId("keycloak");

        return WebClient.builder().filter(oauth).build();
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }
```

リソースサーバへのアクセス方法。
上記のように WebClient をサービスごとに用意していれば、通常通りに WebClient を使えばよい。

```Java
    @Autowired
    @Qualifier("webClientForServiceA")
    private WebClient webClientForServiceA;

    @GetMapping()
    public Mono<String> hello() {
        Mono<String> body =
        webClientForServiceA.get()
            .uri("http://localhost:8081/hello")
            .retrieve()
            .bodyToMono(String.class);
        return body.map(m -> m + ":" + "hello " + new Date());
    }
```

#### CSRF対策

以下のコードを追加することで
Thymeleaf が CSRF対策に対応したHTMLを出力するようになる。
逆の言い方をすれば、以下のコードを追加せずに、CSRF対策を有効にすると（Spring SecurityのデフォルトはCSRFは有効）、
POSTでの画面遷移時に CSRF対策のチェックでNGになってしまうので注意。

```java
import org.springframework.security.web.reactive.result.view.CsrfRequestDataValueProcessor;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

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

以下、2つはパッケージ違いの同名クラスがあるので注意
- `org.springframework.security.web.reactive.result.view.CsrfRequestDataValueProcessor`
- `org.springframework.security.web.server.csrf.CsrfToken`


### リソースサーバ実装

ちなみに、リソースサーバを
アクセストークンで保護するだけの目的であれば、
Keycloak Gatekeeper のようなリバースプロキシ等を前段に配置するだけで
リソースサーバ自身のコードで対応する必要はない。

リソースサーバでトークンの中身を見たい場合は、
対応が必要（ではないかと）。

##### 依存関係

```groovy
implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
```

TODO jwt関連が必要なはず。

#### プロパティ(KeyCloak)

KeyCloak の最小設定。

```
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://<KeyCloakHost>:<KeyCloakPort>/auth/realms/<レルム名>
```

#### コントローラー

- 引数: @AuthenticationPrincipal Jwt
  `jwt.getClaimAsString("preferred_username")` でユーザ名がとれる。
- 引数: @AuthenticationPrincipal OAuth2AuthenticatedPrincipal
  残念ながら取得できず
- 引数: BearerTokenAuthentication
  実行時エラーになる
- 引数: ServerWebExchange  
  `ServerWebExchange.getPrincipal()` で `JwtAuthenticationToken` が取得できる。
  ただし、`Mono` でラップされていることに注意。
  `JwtAuthenticationToken#getPrincipal()` で `@AuthenticationPrincipal` アノテーションを付与した引数で取得できるものが取得できる。
  そもそも、`JwtAuthenticationToken#getToken()` と同じものが取得できるはず。

注意：FQCNは以下のとおり。FQCNが違うと当然動かない。超はまりポイント！
```java
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
```

#### Java Config

以下、jwtでやる場合。

```java
@EnableWebFluxSecurity
public class SecurityConfig {
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        // 認可設定
        http.authorizeExchange()
                .pathMatchers("/health").permitAll()
                .pathMatchers("/message/**").hasAuthority("SCOPE_message:read")
                .anyExchange().authenticated();
        // リソースサーバ
        http.oauth2ResourceServer()
                .jwt();
        return http.build();
    }
}
```

### Security Context

※注意：リアクティブ環境の場合、`SecurityContextHolder.getContext()` では取得できない。

`ReactiveSecurityContextHolder.getContext()` で `SecurityContext` が取得できる。
※FQCN: `org.springframework.security.core.context.SecurityContext`

SecurityContext.getAuthentication().getPrincipal() でコントローラーの
`@AuthenticationPrincipal` アノテーションを付与した引数で取得できるものが取得できる。
ただし、`Mono` でラップされていることに注意。

```java
@GetMapping
public Mono<String> hello(@AuthenticationPrincipal Jwt principal, ServerWebExchange exchange) {
    log.info("★back hello");
    log.info("id={}", principal.getId());
    log.info("tokenValue={}", principal.getTokenValue());
    log.info("claims={}", principal.getClaims());
    log.info("preferred_username={}", principal.getClaimAsString("preferred_username"));
    log.info("subject={}", principal.getSubject());

    var securityContext = SecurityContextHolder.getContext();
    log.info("★securityContext = {}", securityContext);

    var rsc = ReactiveSecurityContextHolder.getContext();
    return rsc.doOnNext(sc -> {
        var auth = sc.getAuthentication();
        var pri = auth.getPrincipal();
        log.info("★authentication class = {}", auth);
        log.info("★principal class = {}", pri.getClass().getName());
    }).then(exchange.getPrincipal())
    .doOnNext(pri -> {
        log.info("★ex pri = {}", pri);
    })
    .map(pri -> {
        return pri;
    }).thenReturn("back hello " + principal);
}
```

