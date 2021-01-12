Flyway
===

### 依存関係

`org.flywaydb:flyway-core` と `org.springframework:spring-jdbc` が必要。
たいていの場合、DBを扱う時点で `org.springframework:spring-jdbc` への依存関係があるため、
追加の依存関係は `org.flywaydb:flyway-core` だけで十分。
ただし、Sprng Data R2DBC は `org.springframework:spring-jdbc` に依存していないため、
Sprng Data R2DBC を使いつつ、 Flyway を使う場合は、
`org.springframework:spring-jdbc` への依存関係も追加する必要がある。

```groovy
implementation 'org.flywaydb:flyway-core'
implementation 'org.springframework:spring-jdbc'
```

### プロパティ
Sprng Data R2DBC を使っている場合の設定。

```
spring.flyway.enabled=true
spring.flyway.url=jdbc:postgresql://localhost:5432/postgres
spring.flyway.schemas=public
spring.flyway.user=${spring.r2dbc.username}
spring.flyway.password=${spring.r2dbc.password}
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1.0.0
```

datasource の定義がある場合は、
いくつか不要な定義もあるはず。

### SQLファイル

`src/main/resources/db/migration` 配下に
`V<VERSION>__<DESCRIPTION>.sql` という命名則のSQLファイルを置く。

