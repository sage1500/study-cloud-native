Spring Cloud Sleuth
===

Spring Cloud Sleuth を使用すると、ログにトレースIDをログに埋めてくれます。
また、このトレースIDはサーバをまたいでも同じIDを付与してくれるため、
ログのトレースが容易になります。

## build.gradle

Spring Cloud Sleuth のスターターを依存関係に追加します。
ただ、その前に、Spring Cloud Sleuth は、その名の通り、Spring Cloud で提供されるライブラリのため、
`dependencyManagement` で Spring Cloud の BOM を取り込む必要があります。

これについては、
[Spring Initializr](https://start.spring.io/) 
で Spring Cloud Sleuth を依存関係に入れた状態の `build.gradle` を生成したものをベースにしてください。

例）Spring Initializr で生成した `build.gradle` から関係がある部分を抜粋:
```groovy
ext {
    set('springCloudVersion', "2020.0.1")
}
dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-sleuth'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
    }
}
```

## `application.properties`

Spring Cloud Sleuth はデフォルトでトレースIDのほかに、
`spring.application.name` で設定したプロパティもログに出力するため、
このプロパティが未設定なら設定した方がよいでしょう。

```
spring.application.name=frontweb
```

## トレースIDのサーバ間連携

Spring Cloud Sleuth はログに埋め込むトレースIDをサーバ間で連携してくれます。
が、あらゆるサーバ間連携方法（通信方法）で、常に自動でやってくれるわけではありません。

詳細は以下参照)  
https://docs.spring.io/spring-cloud-sleuth/docs/current/reference/html/integrations.html#sleuth-integration

- Spring JMS  
    自動でサポートされる。
- WebClient  
    Bean定義した `WebClient` はサポートされる。

WebClient を Spring Cloud Sleuth に対応するためには、
Bean定義する必要がある。
