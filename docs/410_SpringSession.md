Spring Session
===

Spring Session を利用することで
セッションのデータを外部に持たせることができる。

Macchinetta のガイドライン参照

https://macchinetta.github.io/cloud-guideline/current/ja/ImplementationAtEachLayer/SessionManagement.html


なお、Spring Session の対応をする前後で以下の操作をしてみて、対応により挙動がどう変わるか確認してみよう。
- 「ＸＸ確認画面」を表示した状態で、WebAPサーバのプロセスを再起動する。
- 起動後、「ＸＸ完了画面」への遷移する画面操作をする。


Spring Session Jdbc を利用する場合（参考）
---

### 依存関係
依存関係に以下を追加する。

- `org.springframework.session:spring-session-core`
- `org.springframework.session:spring-session-jdbc`
- JDBCを使用する何らかの starter (MyBatisのstarterなど)
- JDBCのドライバ

例)  
```
implementation 'org.springframework.session:spring-session-core'
implementation 'org.springframework.session:spring-session-jdbc'
implementation 'org.springframework.boot:spring-boot-starter-jdbc'
runtimeOnly 'org.postgresql:postgresql'
```

※MyBatisを使用しているなら `org.springframework.boot:spring-boot-starter-jdbc` は不要。

例)  
```
implementation 'org.springframework.session:spring-session-core'
implementation 'org.springframework.session:spring-session-jdbc'
implementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter:2.1.3'
runtimeOnly 'org.postgresql:postgresql'
```

### application.properties
- `spring.session.store-type` に `jdbc` を設定する。  
  これはなくても動いたりするが、依存関係等に誤りがある場合に早期に気が付けるため、設定しておいた方が無難。
- `spring.datasource.*` でデータソースを設定する。

例)  
```
## Spring Session
spring.session.store-type=jdbc

## DataSource(Postgres)
spring.datasource.platform=postgresql
spring.datasource.driverClassName=org.postgresql.Driver
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=postgres
```


Spring Session Redis を利用する場合
---

### 依存関係
依存関係に以下を追加する。

- `org.springframework.session:spring-session-data-redis`
- `org.springframework.boot:spring-boot-starter-data-redis`


例)  
```groovy
implementation 'org.springframework.session:spring-session-data-redis'
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

### application.properties
- `spring.session.store-type` に `redis` を設定する。  
- その他、必要に応じて `spring.redis.*` のプロパティを設定する。

例)  
```
spring.session.store-type=redis
spring.redis.host=localhost
spring.redis.port=6379
```

- その他、Macchinetta のガイドライン参照  
    https://macchinetta.github.io/cloud-guideline/current/ja/ImplementationAtEachLayer/SessionManagement.html


Javaコード実装上の注意
---

セッションの利用方法そのものはTERASOLUNAのガイドライン参照。

http://terasolunaorg.github.io/guideline/5.6.1.RELEASE/ja/ArchitectureInDetail/WebApplicationDetail/SessionManagement.html

### 共通
- セッションに格納するデータは `java.io.Serializable` を実装していること。

### `@SessionAttributes` アノテーションの使用

セッションの外部化が期待通りに動作させるためには、以下の注意が必要。
守られていない場合は Javaコードの修正が必要になる。

1. `@SessionAttributes` で名前指定の場合
    - メソッドに `@ModelAttribute` を付与する際の名前を `@SessionAttributes` の名前に合わせる。
1. `@SessionAttributes` でクラス指定の場合
    - セッション対象のオブジェクトはハンドラ引数に明示的に `@ModelAttribute` の設定が必要。
    - `@ModelAttribute` が付与されたメソッドがあるのはNG。  
    セッション対象のオブジェクトを生成すべきハンドラメソッドで明示的に生成し、Modelの属性に追加する必要あり。

TERASOLUNAのガイドライン上は、オプション的な表記になっているように読めるが、
上記、「`@SessionAttributes` で名前指定の場合」で実装するのがお手軽。

### Spring Framework の sessionスコープのBeanの使用（参考）

Application のクラスに `@ComponentScan(scopedProxy = ScopedProxyMode.TARGET_CLASS)` が必要。

例)  
```Java
@SpringBootApplication
@ComponentScan(scopedProxy = ScopedProxyMode.TARGET_CLASS)
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

Redisを使った場合のセッションの内容確認方法（参考）
---

1. redis-cli で redis に接続する
1. コマンド `keys *` でキー一覧が見れる
    1. `spring:session:sessions:XXXX` というキーが登録されているのがわかるはず
    1. 参考) コマンド `object encoding spring:session:sessions:XXXX` で `hashtable` であることがわかる
1. コマンド `hgetall spring:session:sessions:XXXX` でセッションの中身が見れる。
    1. key と value が交互に出力されている（？）
    1. セッションの属性で保存したものは `sessionAttr:YYYY` というキーで保存されている。
    1. コマンド `hget spring:session:sessions:XXXX sessionAttr:YYYY` で特定のキーだけ内容を確認できる。
