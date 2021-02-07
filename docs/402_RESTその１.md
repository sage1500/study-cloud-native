RESTその１
===

RESTのAPIを実装する方法をについて解説する。
ここでの方針は以下のとおり。

- コードは手組（自動生成しない）
- 認可による保護はしない
- WebFlux を利用

## 依存関係

WebFlux が利用できるようになっていればOK。

## サービス側の実装

ここでは手組での実装方法を詳しく取り扱わない。
もし、RESTのサービス側の実装を詳しく知りたい場合は、TERASOLUNAのガイドラインを
参照してほしい。

- 5.1. RESTful Web Service  
    http://terasolunaorg.github.io/guideline/5.6.1.RELEASE/ja/ArchitectureInDetail/WebServiceDetail/REST.html


RESTのサービス側の実装は、`@ReqeustMapping`や`@GetMapping`など、通常のコントローラーの実装の仕組みがそのまま使える。
実装上の違いは、`@Controller`の代わりに`@RestController`を使用すること。
そうすると、メソッドの返値が JSON に変換されて、サービスの要求元に応答を返すようになる。

```java
@RestController
@RequestMapping("/hello")
@Slf4j
public class HelloController {
    @GetMapping("/")
    public String hello() {
        log.debug("hello");
        return "Hello";
    }
}
```

## クライアント側の実装

RESTのクライアントの通信ライブラリにはいくつか種類がある。

- RestTemplate
- WebClient

以下のTERASOLUNAのガイドラインでは、RestTemplate を使用している。

- 5.2. RESTクライアント（HTTPクライアント）  
    http://terasolunaorg.github.io/guideline/5.6.1.RELEASE/ja/ArchitectureInDetail/WebServiceDetail/RestClient.html

ここでは、ノンブロッキングでやりたいので、
WebClient を使用する。

### WebClient の生成

WebClient を単に使用するだけなら、どこで生成しても構わない。
ただ、後々、Spring Cloud Sleuth を使う関係上、
WebClient は Bean定義する必要があるため、今のうちから Bean定義する方法で生成する。

以下、実装例。

```java
@Configuration
public class HelloApiConfig {
    @Bean
    public WebClient webClientForHello() {
        // @formatter:off
        return WebClient.builder()
                .baseUrl("http://localhost:8081/hello")
                .build();
        // @formatter:on
    }
}
```

ポイント
- 認可による保護やログのためのコードは現時点では未実装
- baseUrl の部分は外部設定に追い出すべき
- メソッド名 `webClientForHello` はどんな名前にしてもよいが、
  Bean名として扱われるため、一意の名前をつけること。

### 呼出し

WebFlux のコントローラーから
WebClient を用いて、RESTサービスを呼び出す手順は
大筋以下のようになる。

1. WebClient を DI で入手
1. 以下のようなコードでサービスを呼出し、`Mono` を取得 
    ```java
    Mono<String> result =
        webClientForHello
            .get() // GET/POST/PUT/DELETE
            .uri("/") // URL
            .retrieve()
            .bodyToMono(String.class); // レスポンスをマッピングするクラス
    ```
1. 取得した `Mono` に対して、`map`等を用いて、中身を取り出す。

具体的なコード例は以下のとおり。

```java
@Controller
@RequestMapping("/home")
@RequiredArgsConstructor
@Slf4j
public class HomeController {
    private final WebClient webClientForHello;
    private final TodosApi todosApi;

    @GetMapping
    public Mono<String> home(Model model) {
        log.debug("[HOME]index");

        return Mono.zip(
                // Hello API
                webClientForHello.get().uri("/").retrieve().bodyToMono(String.class),

                // TODO一覧取得 API
                todosApi.listTodos().collectList())
                //
                .map(results -> {
                    var hello = results.getT1();
                    var todoList = results.getT2();

                    // 複数のAPIの結果をビューに反映
                    model.addAttribute("hello", hello);
                    model.addAttribute("todoList", todoList);

                    return "home";
                });
    }
}
```

ポイント：
- WebClient は複数Bean定義されることが予想されるため、WebClient のフィールド名はBean定義したときのBean名(メソッド名)に合わせる。
- 上記例は、複数の `Mono` を一度に扱うため、`Mono.zip` を使っている。一つだけ扱う場合は、以下のように書けばよい。（はず★現時点では未動作確認）  
    ```java
    @GetMapping
    public Mono<String> home(Model model) {
        return webClientForHello
            .get()
            .uri("/")
            .retrieve()
            .bodyToMono(String.class)
            .map(hello -> {
                model.addAttribute("hello", hello);
                return "home";
            });
    }
    ```
