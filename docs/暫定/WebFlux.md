WebFlux
===

フィルタ
---

### 受信
`WebFilter` をBean定義すればよい。
フィルターの順序は `@Order` で大まかに定義できる。

ただ、`WebFilter` は
すべてのリクエストに対してフィルターがかかってしまうのが難点。

→staticなものにもかかるのが難点だが、staticなものは、そもそも別サーバで処理すればOK  
→それ以外ですべてにフィルターがかかっても気にならないはず。
コントローラー単位でフィルタを変更したいケースの多くは昔はセキュリティ系だったが、いまは別枠でフィルターかかるので気にならないはず。

自前でフィルタを書けるか判定処理を記述する必要あり（？）

```Java
@Configuration
@Slf4j
public class LoggingFilterConfig {
    @Bean
    public WebFilter loggingFilter() {
        return (exchange, chain) -> chain.filter(exchange).transformDeferred(call -> Mono.fromRunnable(() -> {
            // Before
            var req = exchange.getRequest();
            log.info("★before request.cookies={}", req.getCookies());
            log.info("★before request.headers={}", req.getHeaders());
        }).then(call).doOnSuccess(done -> {
            // after (success)
            var rsp = exchange.getResponse();
            log.info("★after success response.statusCode={}", rsp.getStatusCode());
            log.info("★after success response.cookies={}", rsp.getCookies());
            log.info("★after success response.headers={}", rsp.getHeaders());
        }).doOnError(throwable -> {
            // after (with error)
            log.info("★after error");
        }));
    }
}
```

以下のように書けなくもないけど、（たぶん、）上記の書き方とは挙動が違う。

```Java
@Configuration
@Slf4j
public class LoggingFilterConfig {
    @Bean
    public WebFilter loggingFilter() {
        return (exchange, chain) -> {
            var req = exchange.getRequest();
            log.info("★★before request.cookies={}", req.getCookies());
            log.info("★★before request.headers={}", req.getHeaders());
            return chain.filter(exchange);
        };
    }
}
```

`@Component` で定義した方が素直かも？
```java
@Component
@Slf4j
public class WebLoggingFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (exchange.getRequest().getPath().pathWithinApplication().value().startsWith("/css/")) {
            return chain.filter(exchange);
        }

        return chain.filter(exchange).transformDeferred(call -> Mono.fromRunnable(() -> {
            // Before
            var req = exchange.getRequest();
            log.info("BEFORE REQUEST: {} {} query={}", req.getMethod(), req.getURI(), req.getQueryParams());
            // log.info("★before request.cookies={}", req.getCookies());
            // log.info("★before request.headers={}", req.getHeaders());
        }).then(call).doOnSuccess(done -> {
            // after (success)
            var rsp = exchange.getResponse();
            log.info("AFTER SUCCESS: statusCode={}", rsp.getStatusCode());
        }).doOnError(throwable -> {
            // after (with error)
            log.info("AFTER ERROR: {}", throwable, throwable.getMessage());
        }));
    }
}
```


### 送信(WebClient)

`ExchangeFilterFunction` を定義して、
`WebClient.builder().filter()` で登録してやればよい。

```java
@Component
@Slf4j
public class WebClientLoggingFilter implements ExchangeFilterFunction {
    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return next.exchange(request).transformDeferred(call -> Mono.fromRunnable(() -> {
            // Before
            log.info("[WEB-CLIENT]REQUEST: {} {}", request.method(), request.url());
        }).then(call).doOnSuccess(done -> {
            // after (success)
            log.info("[WEB-CLIENT]SUCCESS: statusCode={}", done.statusCode());
        }).doOnError(throwable -> {
            // after (with error)
            log.info("[WEB-CLIENT]ERROR: {}", throwable, throwable.getMessage());
        }));
    }
}
```

```java
    @Bean
    public WebClient webClientForTodo(WebClientLoggingFilter loggingFilter) {
        // @formatter:off
        return WebClient.builder()
                .baseUrl("http://localhost:8082/todos")
                .filter(loggingFilter)
                .build();
        // @formatter:on
    }
```


### Ruter Function

以下のように書いて @Controller でのアノテーションを RouterFunction に分離する感じ。
一般的には、controller部分とrouter部分は別クラスにするっぽいが。

- String でビュー名を返す代わりに render(ビュー名) とする
- リダイレクトは以下のいずれかを使う。
  - status(HttpStatus.MOVED_PERMANENTLY).location(URI)
  - permanentRedirect(URI)
  - temporaryRedirect(URI)
- TODO リダイレクト時の flushAttribute(?) はどうやる？

```java
@Configuration
@Slf4j
public class ReactTestController {

    public Mono<ServerResponse> test(ServerRequest req) {
        var attrs = new HashMap<String, Object>();
        attrs.put("hello", "Hello world.");
        return ServerResponse.ok().render("home", attrs);
    }

    public Mono<ServerResponse> test2(ServerRequest req) {
        return ServerResponse.permanentRedirect(URI.create("/home")).build();
    }

    @Bean
    public RouterFunction<ServerResponse> reactTestRouter() {
        return RouterFunctions.route()
            .GET("/test", this::test)
            .GET("/test2", this::test2)
            .build();        
    }
}
```

### その他

- Reactive Stream の結果が途中で `Mono<Void>` になると、うまく動かない。
  そのため、APIの呼出し結果が `Mono<Void>` な API をうまく扱えない。

### TODO

- Thymeleaf で form に CSRF対策のパラメータが追加されない
- post時に RequestMapping の params での振り分けができない。@RequestParam でパラメータが取れない。

→Bodyの内容が確定していることを前提としたSpringMVC由来の挙動は動作しない？
  →意外なことに 引数に Form を設定すると入手できる。


自前で振り分けてみる？

```java
    @RequestMapping(method = { RequestMethod.POST, RequestMethod.GET})
    public Mono<String> index(ServerWebExchange exchange) {
        log.info("★index2");
        return exchange.getFormData().flatMap(m -> {
            log.info("formdata={}", m);
            if (m.containsKey("create")) {
                return create();
            } else if (m.containsKey("update")) {
                return update();
            } else if (m.containsKey("delete")) {
                return delete();
            } else {
                return Mono.just("redirect:?list");
            }
        });
    }
```

JavaScript が前提になるが、JavaScript で action のパスを切り替えるのが素直？

そもそも form を分けるというのも手。


- RedirectAttributes がない。PRGパターンはどうする？
  - @SessionAttributes に属性名を登録する。
    - 引継ぎ元: Model.addAttribute() する。
    - 引継ぎ先: @SessionAttribute()アノテーションを引数に付与する。modelには設定済なので再設定は不要。
      ```java
        @Controller
        @RequestMapping("/hoge")
        @SessionAttributes("message")
        @Slf4j
        public class TodoCreateController {
            // ...
            @PostMapping("execute")
            public String execute(TodoForm todoForm, Model model) {
                model.addAttribute("message", "test");
                return "redirect:complete";
            }

            @GetMapping("complete")
            public String complete(SessionStatus sessionStatus, @SessionAttribute("message") String message) {
                log.info("complete: message={}", message);
                sessionStatus.setComplete();
                return "todo/todoCreateComplete";
            }
        }
      ```
  - 渡すものが文字列であれば、GET の Quertyパラメータで返す。GET なので WebFlux でも大丈夫。ブラウザのURL欄にパラメータが見えてしまうのを気にしなければこれでもOK。
    @RequestParam()アノテーションを引数に付与して、modelに設定する。
    ```java
    @PostMapping("execute")
    public Rendering execute(TodoForm todoForm) {
        log.info("todo create execute: {}", todoForm);
        return Rendering.redirectTo("complete?message={message}").modelAttribute("message", "test4").build();
    }

    @GetMapping("complete")
    public String complete(SessionStatus sessionStatus, @RequestParam("message") String message) {
        log.info("todo create complete: message={}", message);
        sessionStatus.setComplete();
        model.addAttribute("message", message);
        return "todo/todoCreateComplete";
    }
    ```
