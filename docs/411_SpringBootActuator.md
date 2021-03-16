Spring Boot Actuator
===

Spring Boot Actuator を使用すると、アプリケーションのヘルスチェックを簡単に実装できる。

ここでは、Spring Boot Actuator はヘルスチェック以外にもアプリケーションの監視機能を持っているが、
ヘルスチェックのみ有効にし、ほかの機能は無効にすることとする。

参考）  
https://spring.pleiades.io/spring-boot/docs/current/reference/html/production-ready-features.html#production-ready


ヘルスチェックの機能を有効にすると、
以下のようなコマンドでアプリケーションのヘルスチェックができる。

```
> curl.exe -v localhost:8080/actuator/health
```

## Java Config

Spring Security を使用している場合、
通常はHTTPでの要求に対して認証済のみアクセスを許可するように設定する。
したがって、そのままだとヘルスチェック用のパスに対しても認証済であることが求められてしまう。
そのため Spring Boot Actuator 用のパスは認証済でなくてもアクセスを許可するように
Spring Security の Java Config の設定を変更する。

例）
```java
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        // 認可設定
        // @formatter:off
        http.authorizeExchange()
                .pathMatchers("/manage/**").permitAll()  // この行を追加。なお、この例は、パスをデフォルトの actuator から manage に変更していること前提としている。
                .anyExchange().authenticated();
        // @formatter:on

        // リソースサーバ
        http.oauth2ResourceServer().jwt();
        return http.build();
    }
```

## 依存関係

Spring Boot Actuator を依存関係に追加する。

```groovy
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

## `application.properties`

### パス変更

Spring Boot Actuator 関連の URL のパスをデフォルトの `/actuator` から変更したい場合は、
`management.endpoints.web.base-path` に変更後のパスを設定する。

```
management.endpoints.web.base-path=/manage
```

### ヘルスチェックのみ有効化

ヘルスチェックのみ有効にする。
そのため、デフォルトですべて無効にし、個別にヘルスチェックの機能のみ有効にするように設定する。

```
management.endpoints.enabled-by-default=false
management.endpoint.health.enabled=true
```

### ヘルスチェックの対象を設定

ヘルスチェックの機能を有効にすると、デフォルトでは、アプリケーションのプロセスだけでなく、依存関係に DB や Redis などが含まれていると、
それらのサービス状態もヘルスチェックの対象にしてくれる。
この動作が余計な場合は、`management.health.defaults.enabled` を `false` にし、
有効にしたいチェックのみ `management.health.XXX.enabled` を `true` に設定する
（`XXX`の部分にはチェックごとのキーを記載する）。

なお、`XXX` として、`ping` は自身のアプリケーションのヘルスチェックのキーとなっているため、一般的に `ping` は有効にする。

例）
```
management.health.defaults.enabled=false
management.health.ping.enabled=true
management.health.db.enabled=true
```

`XXX` の部分に何が設定できるかは以下参照。  

https://spring.pleiades.io/spring-boot/docs/current/reference/html/production-ready-features.html#production-ready-health-indicators

ただし、上記資料は最新化されていない。
実際、上記の例では `db` と記載している部分は、
上記のサイトでは、`datasource` になっている（正解は `db`）。

そのため、実際に何が設定できるか調べたい場合は現状では、Spring Boot Actuator のソースコードを見た方が確実である。
調べ方は、以下のソースから各 `XxxAutoConfiguration.java` という名前のファイルを開き、
`@ConditionalOnEnabledHealthIndicator("XXX")` の `XXX` の部分を確認する。

https://github.com/spring-projects/spring-boot/tree/master/spring-boot-project/spring-boot-actuator-autoconfigure/src/main/java/org/springframework/boot/actuate/autoconfigure

たとえば、
`r2dbc/ConnectionFactoryHealthContributorAutoConfiguration.java`
を見ると、`@ConditionalOnEnabledHealthIndicator("r2dbc")` と記載があるため、
R2DBCを用いた DBアクセスを実装している場合の DBのヘルスチェックの `XXX` に当たる部分は、`r2dbc` だとわかる。


## その他参考

### 独自のヘルスチェックの実装

独自のヘルスチェックを実装する場合は、
`HealthIndicator` を実装するか、`AbstractHealthIndicator` を拡張する。
また、それぞれのリアクティブ版として、`ReactiveHealthIndicator` と `AbstractReactiveHealthIndicator` も用意されている。

これらのクラスを Bean 定義すると、Bean名の最後の `HealthIndicator` を除いた部分の名前で
`HealthContributorRegistry` （リアクティブの場合は、`ReactiveHealthContributorRegistry`）に
独自のヘルスインジケータとして自動で登録される。
そして、この名前は `application.properties` で指定するために利用する。

`@Component` の動きとして、Bean名を設定しなかった場合のデフォルトのBean名はクラス名の先頭一文字を小文字にした名前になる。
これらを踏まえると、`@Component`付きの独自のヘルスチェックを実装するクラスの名は `XyzHealthIndicator` にするとよい。この場合、`xyz` という名前で `application.properties` で設定することができるようになる。


`HealthIndicator`の実装例）
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

`AbstractHealthIndicator`の拡張例：枠組のみ）
```Java
@Component
public class MyHealthIndicator extends AbstractHealthIndicator {
    @Override
    protected void doHealthCheck(Builder builder) throws Exception {
        // TODO Auto-generated method stub
    }
}
```

`ReactiveHealthIndicator`の実装例：枠組のみ）
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

`AbstractReactiveHealthIndicator`の拡張例：枠組のみ）
```Java
@Component
public class MyHealthIndicator extends AbstractReactiveHealthIndicator {
    @Override
    protected Mono<Health> doHealthCheck(Builder builder) {
        return Mono.just(builder.up().build());
    }
   
}
```

### Health.Status と HTTPステータス

Health.Status と HTTPステータスの関係はデフォルトでは、以下のとおり。

- `OUT_OF_SERVICE`: 503
- `DOWN`: 503
- `UP`: 200

curlコマンドは HTTPステータスが400以上だと、終了コード 22 を返す。
つまり、`UP` 以外は curl コマンドはエラー終了した扱いになる。
Docker の `health-cmd` に curlコマンドを設定することができる。

### Kubernetes向けサポート

Kubernetes の livenessProbe および readinessProbe 用に
`/actuator/health/liveness` および `/actuator/health/readiness` というパスが用意されている。
Kubernetes 環境だと自動的に有効になる（未検証）。

Kubernetes 環境出ない場合でも、
```
management.endpoint.health.probes.enabled=true
```
で強制的に有効にすることもできる。


`/actuator/health/liveness` は livenessグループ、
`/actuator/health/readiness` は readinessグループの HealthIndicator が使われる。
これらのグループにはデフォルトでは、
`LivenessStateHealthIndicator` および `ReadinessStateHealthIndicator` だけ存在する。
これ以外の HealthIndicator を各Probeの対象にしたい場合は、livenessグループおよびreadinessグループへの登録が必要。

たとえば、独自に `MyHealthIndicator` というクラスを作り、これを `/actuator/health/liveness` および `/actuator/health/readiness` アクセス時に
評価されるようにしたい場合は以下のように設定する。

```
management.endpoint.health.probes.enabled=true
management.endpoint.health.group.liveness.include=livenessState,my
management.endpoint.health.group.readiness.include=readinessState,my
```
