RESTその２
===

RESTのAPIを実装する方法をについて解説する。
ここでの方針は以下のとおり。

- コードは自動生成
- 認可による保護はしない
- WebFlux を利用

コードの自動生成には、OpenAPI Generator を使用する。
OpenAPI Generator には、Gradle のプラグインがあり、このプラグインを利用することで
Gradle から REST用のコードを自動生成することができる。

## OASの用意

OAS については、
[403_OpenAPIGenerator](403_OpenAPIGenerator.md) 参照。

## ビルドファイル

`build.gradle` の修正方法は、
[403_OpenAPIGenerator](403_OpenAPIGenerator.md) 参照。


## サービス側の実装

OpenAPI Generator で生成したコードにコントローラーのインタフェースが含まれているので
このインタフェースを実装する。

```java
@RestController
@RequiredArgsConstructor
@Slf4j
public class TodoController implements TodosApi {
	// ...
}
```

一覧系のメソッドハンドラの実装例。

だいたい、以下のような実装になる。

1. `Flux<T>` を返す一覧取得系のメソッドを呼び出す。
1. `Flux<T>` を `Flux<U>` に変換する。
    - `map` を使って、`T` を取り出し、`U` に変換する。
1. `Flux<U>` を `Mono<ResponseEntity<Flux<U>>>` に変換する。
    - `as` を使って `Flux<U>` そのものを取得し、 `ResponseEntity<Flux<U>>` に変換する。

```java
    @Override
    public Mono<ResponseEntity<Flux<TodoResource>>> listTodos(ServerWebExchange exchange) {
		String userId = "user1";
        // @formatter:off
        return todoService.findAllByUserId(userId)  // 返値が Flux<Todo> のサービス
            .map(todo -> toTodoResource(todo)) // Todoエンティティを REST用の TodoResource に変換
            .as(f -> Mono.just(ResponseEntity.ok(f)));
        // @formatter:on
    }
```

リソースを受け取るメソッドハンドラの実装例。

だいたい、以下のような実装になる。

1. `Mono<T>` を `Mono<U>` に変換する。  
    やり方としては以下のパターンがある。
    - `map` を使って、`T` を取り出し、`U` に変換する。
    - `flatMap` を使って、`T` を取出し、`Mono<U>` に変換する（`T` を用いて、`Mono<U>` を返すメソッドを呼び出す）。
1. `Mono<U>` を `Mono<ResponseEntity<U>>` に変換する。
    - `map` を使って、`U` を取り出し、`ResponseEntity<U>` に変換する。

```java
    @Override
    public Mono<ResponseEntity<TodoResource>> createTodo(@Valid Mono<TodoResource> todoResource,
            ServerWebExchange exchange) {
        // @formatter:off
        return todoResource.flatMap(rsrc -> { // Monoからデータを取り出し、Mono を返す。
				String todoId = null;
				Todo todo = toTodo(rsrc, todoId);  // REST用の TodoResourceをTodoエンティティに変換
                return todoService.save(todo); // DBに保存。返値は Mono<Todo>
            })
            .map(todo -> ResponseEntity.ok(toTodoResource(todo)));
        // @formatter:on
	}
```

例外を処理したい場合は、`onErrorResume` を使用する。

```java
    @Override
    public Mono<ResponseEntity<TodoResource>> updateTodo(String todoId, @Valid Mono<TodoResource> todoResource,
            ServerWebExchange exchange) {
        // @formatter:off
        return todoResource.flatMap(rsrc -> {
				Todo todo = toTodo(rsrc, todoId);  // REST用の TodoResourceをTodoエンティティに変換
                return todoService.save(todo); // DBに保存。返値は Mono<Todo>
            })
            .map(todo -> ResponseEntity.ok(toTodoResource(todo)))
            // 楽観的ロックNGの場合
            .onErrorResume(OptimisticLockingFailureException.class, e -> {
                log.error("[TODO]楽観的ロックNG: {}", e.getMessage());   
                return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
            });
        // @formatter:on
    }
```

## クライアント側の実装

クライアント側の実装は OAS から生成したクライアントコードのクラスを Bean定義する部分と、
それを利用する部分が必要。

### 生成したクライアント側のクラスを Bean 定義する。

生成したクライアント側のクラスのインスタンスを直接生成だけなら単純。
ただし、後々、Spring Cloud Sleuth を使う関係上、
クライアント側のクラスが内部で使用する WebClient を Bean定義する必要がある。
後々改善されるかもしれないが、現在の生成されるコードでは、
WebClient を Bean定義するのがちょっと面倒。

大まかには以下のような実装になる。

1. REST APIサービス用の `WebClient` を Bean定義する。
    - この際、`WebClient` の `exchangeStrategies` の設定を OAS から生成されたコードと同じ実装にする（基本的にはコピーする）。
1. クライアント側のクラスを Bean定義する。
    - Bean定義した `WebClient` を用いて、クライアント側のクラスを生成する。

```java
@Configuration
public class TodosApiConfig {

    @Bean
    public WebClient webClientForTodo() {
        ExchangeStrategies strategies = createExchangeStrategiesForTodosApi();

        // @formatter:off
        return WebClient.builder()
                .baseUrl("http://localhost:8082/todos")
                .exchangeStrategies(strategies)
                .build();
        // @formatter:on
    }

    @Bean
    public TodosApi todosApi(@Qualifier("webClientForTodo") WebClient webClient) { // WebClient は複数定義されている可能性あるため、Bean名を `@Qualifier` で明記する。
        var clientProto = new ApiClient();
        var apiClient = new ApiClient(webClient, null, clientProto.getDateFormat());
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
```

### DI して利用する。

1. Bean定義したクライアント側のクラスを DI する。
1. DI したクラスを呼び出す。
    - 呼出し結果の返値は `Mono<T>` なので、返値に対しては、`WebClient` を用いた場合と同様に Reactor にしたがって実装する。

```java
@Controller
@RequestMapping("/todo")
@RequiredArgsConstructor
@Slf4j
public class TodoListController {
    private final TodosApi todosApi;

    @GetMapping("/")
    public Mono<String> index(Model model) {
        log.debug("[TODO]index");

        // TODO一覧取得API呼出し
        return todosApi.listTodos().collectList().map(list -> {
            // 結果をビューに反映
            model.addAttribute("list", list);
            return "todo/todoList";
        });
    }
}
```
