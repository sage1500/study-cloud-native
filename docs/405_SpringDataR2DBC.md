Spring Data R2DBC
===

Spring Data R2DBC は、Spring Data シリーズの一つです。
Spring Data R2DBC を用いることで、DBアクセスを Reactive Streams に対応させることができます。

Spring Data R2DBC で利用可能なDBは、
JDBCドライバのほかに、R2DBCドライバが提供されている必要があります。
[Spring Initializr](https://start.spring.io/) で確認できる R2DBCドライバが提供されているDBは以下のとおりです。

- H2 
- MariaDB
- MS SQL Server
- MySQL
- PostgreSQL

なお、上記に Oraclde がありませんが、
「Oracle JDBC Thinドライバー21c以降」でサポートされているようです。  
参考）
https://docs.oracle.com/en/database/oracle/oracle-database/21/jjdbc/jdbc-reactive-extensions.html#GUID-10293AD5-C9AD-4B99-8C53-879072307D76


## 依存関係

R2DBCのスターターと、
JDBCおよびR2DBCドライバの依存関係を追加します。

```groovy
implementation 'org.springframework.boot:spring-boot-starter-data-r2dbc'
runtimeOnly 'io.r2dbc:r2dbc-postgresql'
runtimeOnly 'org.postgresql:postgresql'
```

## `application.properties`

`spring.r2dbc.xxx` のいくつかのプロパティを設定します。

```
spring.r2dbc.url=r2dbc:pool:postgresql://localhost:5432/postgres
spring.r2dbc.username=postgres
spring.r2dbc.password=postgres
```

JDBCの場合と異なり、`url` が `jdbc:` から始まるのではなく、
`r2dbc:pool:` から始まるのと少し異なりますが、
基本は、JDBCの場合と同様のに設定します。

## エンティティクラスの実装

Spring Data JDBC と同じです。

```java
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import lombok.Data;

@Data
public class Todo {
    private String userId;
    @Id
    private String todoId;
    private String todoTitle;
    @Version
    private long version;
}
```

## インタフェースの定義

基本は、Spring Data JDBC と同じです。
大きく、以下が異なります。

- `～Repository` の代わりに `Reactive`から始まる `Reactive～Repository` を拡張する。  
    例) `CrudRepository` の代わりに `ReactiveCrudRepository` を拡張する。
- メソッドの返値は `Flux<T>` または `Mono<T>` にする。

```java
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TodoRepository extends ReactiveCrudRepository<Todo, String> {
    Flux<Todo> findAllByUserId(String userId);
    Mono<Todo> findByUserIdAndTodoId(String userId, String todoId);
    Mono<Boolean> deleteByUserIdAndTodoId(String userId, String todoId);
}
```

## サービスの実装

基本は以下のとおり。

- リポジトリの実装を DI で取得する。
- メソッドの返値は `Flux<T>` または `Mono<T>` にする。
- トランザクションを有効にしたい場合は、クラスまたはメソッドに`@Transactional`アノテーションを付与する。

```java
@Service
@Transactional
@RequiredArgsConstructor
public class TodoServiceImpl implements TodoService {
    private final TodoRepository todoRepository;

    @Override
    public Mono<Todo> save(Todo entity) {
        // 追加時はIDを割り振る
        if (entity.getTodoId() == null) {
            entity.setTodoId(UUID.randomUUID().toString());
            entity.setVersion(0);
        }

        return todoRepository.save(entity);
    }

    @Override
    public Flux<Todo> findAllByUserId(String userId) {
        return todoRepository.findAllByUserId(userId);
    }

    @Override
    public Mono<Todo> findByUserIdAndTodoId(String userId, String todoId) {
        return todoRepository.findByUserIdAndTodoId(userId, todoId);
    }

    @Override
    public Mono<Boolean> deleteByUserIdAndTodoId(String userId, String todoId) {
        return todoRepository.deleteByUserIdAndTodoId(userId, todoId);
    }
}

```