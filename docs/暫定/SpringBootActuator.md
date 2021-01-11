Spring Boot Actuator
===

### memo

https://spring.pleiades.io/spring-boot/docs/current/reference/html/production-ready-features.html#production-ready
https://spring.pleiades.io/spring-boot/docs/current/actuator-api/html/

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
}
```

health だけ有効にする  
```
management.endpoints.enabled-by-default=false
management.endpoint.health.enabled=true
```

ヘルスチェックとして利用するだけなら、これでOK。

以下のURLにアクセスしてみるとよい。

http://localhost:8080/actuator/health


actuator のポート番号やコンテキストパスを変える。
多分、以下。未検証。
```
management.server.port=18080
management.server.servlet.context-path=/manage
```

```
## Actuator
management.server.port=8081
management.server.servlet.context-path=/  →警告によると management.server.base-path らしい
management.endpoints.web.base-path=/manage
management.endpoints.enabled-by-default=false

### health
management.endpoint.health.enabled=true
management.endpoint.health.probes.enabled=true
management.endpoint.health.group.liveness.include=livenessState,my
management.endpoint.health.group.readiness.include=readinessState,my
management.health.defaults.enabled=false
management.health.ping.enabled=true
management.health.db.enabled=false

## DataSource(Postgres)
spring.datasource.platform=postgresql
spring.datasource.driverClassName=org.postgresql.Driver
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=postgres
```


2.8. ヘルス情報
2.8.1. 自動構成された HealthIndicators

関連するサービスがダウンしていたらヘルスチェックNGとする考え方がある。
標準でいくつかのサービスの状態をチェックする機能が提供されている。

```
management.health.defaults.enabled=false
management.health.ping.enabled=true
management.health.db.enabled=true
```

たとえば、
`management.health.db.enabled=true` にすると、datasource の定義を参照して、
DBの状態をチェックしてくれる。

※独自の HealthIndicator は、`management.health.defaults.enabled` や`management.health.key.enabled` で有効／無効は制御できない（？）。

TODO AWS向けのはある？→Spring Cloud AWS Actuator というのがあるっぽい。


2.8.2. カスタム HealthIndicators の作成
2.8.3. リアクティブヘルスインジケータ

`HealthIndicator` を実装するか、`AbstractHealthIndicator` を拡張する。
それぞれのリアクティブ版として、`ReactiveHealthIndicator` と `AbstractReactiveHealthIndicator` もある。

```Java
@Component
public class MyHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        int errorCode = check(); // perform some specific health check
        if (errorCode != 0) {
            return Health.down().withDetail("Error Code", errorCode).build();
        }
        return Health.up().build();
    }
}
```

```Java
@Component
public class MyHealthIndicator extends AbstractHealthIndicator {
    @Override
    protected void doHealthCheck(Builder builder) throws Exception {
        // TODO Auto-generated method stub
    }
}
```

```Java
@Component
public class MyHealthIndicator implements ReactiveHealthIndicator {
    @Override
    public Mono<Health> health() {
        // TODO Auto-generated method stub
        return null;
    }
}
```

```Java
@Component
public class MyHealthIndicator extends AbstractReactiveHealthIndicator {
    @Override
    protected Mono<Health> doHealthCheck(Builder builder) {
        return Mono.just(builder.up().build());
    }
   
}
```

Health.Status と HTTPステータスの関係はデフォルトでは、以下のとおり。

- `OUT_OF_SERVICE`: 503
- `DOWN`: 503
- `UP`: 200

curlコマンドは HTTPステータスが400以上だと、終了コード 22 を返す。
Docker の health-cmd で curlコマンドを利用できる。


HealthContributorRegistry を使用して、実行時にヘルスインジケータを登録および登録解除する。
リアクティブの場合は、ReactiveHealthContributorRegistry を使う。

HealthContributorRegistry および ReactiveHealthContributorRegistry できる。
`@Component` つけたクラスの場合は、自動で登録されるため不要。
その場合の登録名は？
Bean名(通常、クラス名の先頭一文字を小文字にしたもの)の末尾から HealthIndicator を除いたもの。

```Java
@Component
@RequiredArgsConstructor
public class MyHealthIndicatorInitializer {
    private final ReactiveHealthContributorRegistry reactiveRegistry;

    @PostConstruct
    public void init() {
        var myCheck = new MyHealthIndicator();
        reactiveRegistry.registerContributor("myCheck", myCheck);
    }
}
```



2.9. Kubernetes プローブ
2.9.1. Kubernetes プローブを使用した外部状態の確認
2.9.2. アプリケーションのライフサイクルとプローブの状態

Kubernetes 向けのサポートがある。

Kubernetes の livenessProbe および readinessProbe 用に
"/actuator/health/liveness" および "/actuator/health/readiness" がある。
Kubernetes 環境だと自動的に有効になる（未検証）。

```
management.endpoint.health.probes.enabled=true
```
で強制的に有効にすることもできる。


"/actuator/health/liveness" は livenessグループ、
"/actuator/health/readiness" は readinessグループの HealthIndicator が使われる。
これらのグループにはデフォルトでは、
LivenessStateHealthIndicator および ReadinessStateHealthIndicator だけ存在するため、
これ以外の HealthIndicator を対象にしたい場合は、livenessグループおよびreadinessグループへの登録が必要。

```
management.endpoint.health.group.readiness.include=readinessState,customCheck
```

3.1. 管理エンドポイントパスのカスタマイズ
3.2. 管理サーバーポートのカスタマイズ

```
management.endpoints.web.base-path=/manage
```

注意：
- `management.endpoints.web.base-path` は
  `server.servlet.context-path` に相対的。
  ただし、`management.server.port` を設定している場合は
  `management.server.servlet.context-path` に対して相対的。

TODO `management.server.servlet.context` が効かない。
TODO WebFlux の場合、`spring.webflux.base-path` 相対にもならない。`management.endpoints.web.base-path` からの絶対パスになる？（バグ？）
TODO WebFlux の場合、`server.servlet.context-path` ではなく、`spring.webflux.base-path` を使うというのも微妙。

エンドポイントを別のパスにマップする場合は、management.endpoints.web.path-mapping プロパティを使用する。

```
management.endpoints.web.path-mapping.health=healthcheck
```



### その他
- redis が落ちているときのタイムアウトが長すぎる。

