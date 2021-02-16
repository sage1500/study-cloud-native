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

## クライアント側の実装

TODO

### WebClient の生成

TODO

### 呼出し

TODO
