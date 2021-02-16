OpenAPI Generator
===

[OpenAPI Generator](https://github.com/OpenAPITools/openapi-generator) は、
[OpenAPI Specification (OAS)](https://github.com/OAI/OpenAPI-Specification)
で定義されたインタフェースから
各種プログラミング言語やフレームワーク用のコードを生成するツールです。
OAS は REST API のサポートを含んでいるため、
OpenAPI Generator を利用することで、
REST API用のコードを OAS から生成することができます。

また、[OpenAPI Generator Gradle Plugin](https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator-gradle-plugin) は、
Gralde のプラグインの一つで、このプラグインを組み込むことで、
OpenAPI Generator によるコード生成を Gradle から利用できるようになります。

ということで、ここでは、OpenAPI Generator Gradle Plugin を用いて、
OAS から REST APIサービス用のコードを生成する方法を紹介します。

## OAS

OAS からコード生成するには、OASのファイルを用意する必要があります。
ファイルの仕様は OAS のページからたどれます。

- 仕様  
	https://github.com/OAI/OpenAPI-Specification/tree/master/versions
- サンプル  
	https://github.com/OAI/OpenAPI-Specification/tree/master/examples/v3.0

上記サンプルにある `petstore-expanded.yaml` が比較的素直な CRUD を実現しているため、
これから OAS を自作する場合は、`petstore-expanded.yaml` をベースに改造するとよいでしょう。

※注  
- 現時点では、返値がない REST API を WebClient で正しく扱う方法を検証できていません。
	返値がない、つまり、Javaコードに変換した場合に返値型が `Mono<Void>` になる場合に
	WebClient の利用者側での Reactive Stream の処理が途中で止まります。
	そのため、現時点では、boolean 等、何かしら値を返すインタフェースとして定義してください。  
	  
	上記の `petstore-expanded.yaml` の場合、`paths.'/pets/{id}'.delete.responses.'204'` のように `content` の記載がない API が返値がない API です。たとえば、以下のような記述を `paths.'/pets/{id}'.delete.responses.'204'` 配下に追加すると、boolean を返すAPIになります。
	```yaml
	content:
		application/json:
			schema:
				type: boolean
	```


## `build.gradle` の設定

OpenAPI Generator Gradle Plugin の使い方自体は、
[OpenAPI Generator Gradle Plugin](https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator-gradle-plugin) のページに書いてあります。
詳細は、そちらを参照してもらうとして、
ここでは、`build.gradle` の基本的な設定方法を紹介します。

### OpenAPI Generator Gradle Plugin の組み込み

`plugins` に OpenAPI Generator Gradle Plugin を設定します。

```groovy
plugins {
	// ...

    // ADD
    id "org.openapi.generator" version "4.2.1"
}
```

### 依存関係の追加

OpenAPI Generator が生成したコードをコンパイルするのに
必要な依存関係を追加します。
実際に、何が必要かは生成されたコードを見て（コンパイルのエラー内容等から）判断する必要があります。

本ドキュメント執筆時点で
`generatorName` に `java` および `spring` を設定した際に
必要となった依存関係は以下のとおりです。

```groovy
dependencies {
    // ...

	// ADD 生成されたソースコードのビルドに必要
	implementation "org.springframework.boot:spring-boot-starter-validation"
	compile "io.springfox:springfox-swagger-ui:2.9.2"
    compile "io.springfox:springfox-swagger2:2.9.2"
	compile "org.openapitools:jackson-databind-nullable:0.2.1"

    // ADD クライアントコード生成時のビルドに必要
  	compile "com.google.code.findbugs:jsr305:3.0.2"
}
```
※`compile`の部分は、`implementation` でいいかも？

### コード生成のタスク定義
OASからコードを生成するタスクのタスク名は標準では `openApiGenerate` です。
ただ、このタスクでは、一つの OAS しか変換できません。

ここでは、1つのプロジェクトで複数の OAS からコード生成する場合を想定しています。

変換元の OAS の数だけ、以下の定義を追加します。

- OASからコードを生成するタスクを定義する。
- 上記タスクが Javaのコンパイル前に動作するようにする。
- 生成したコードがコンパイルされるようにする。

```groovy
// 以下の buildTodoServer の部分は任意の名前
task buildTodoServer(type: org.openapitools.generator.gradle.plugin.tasks.GenerateTask){
    // ...詳細は後述
}

// compileJava のタスク実行前に buildTodoServer タスクを実行する。
compileJava.dependsOn tasks.buildTodoServer

// 生成したコードの出力先をソースフォルダに設定（コンパイル対象にするために）
sourceSets.main.java.srcDir "${buildTodoServer.outputDir.get()}/src/main/java"
sourceSets.main.resources.srcDir "${buildTodoServer.outputDir.get()}/src/main/resources"
```

コードを自動生成するタスクの定義の例。

サーバ側の例。
```groovy
task buildTodoServer(type: org.openapitools.generator.gradle.plugin.tasks.GenerateTask){
    generatorName = "spring"
    inputSpec = "$projectDir/src/main/spec/todo.yml"
    outputDir = "$buildDir/generated"
	modelNameSuffix = "Resource"
	configOptions = [
		apiPackage: 'com.example.api.todo.server.api',
		invokerPackage: 'com.example.api.todo.server.invoker',
		modelPackage: 'com.example.api.todo.server.model',
		dateLibrary: 'legacy',
		java8: 'true',
		reactive: 'true',
		interfaceOnly: 'true',
	]
    systemProperties = [
        modelDocs: 'false'
    ]
}
```

クライアント側の例。
```groovy
task buildTodoClient(type: org.openapitools.generator.gradle.plugin.tasks.GenerateTask){
    generatorName = "java"
    inputSpec = "$projectDir/src/main/spec/todo.yml"
    outputDir = "$buildDir/generated"
	modelNameSuffix = "Resource"
	configOptions = [
		apiPackage: 'com.example.api.frontweb.client.api',
		invokerPackage: 'com.example.api.frontweb.client.invoker',
		modelPackage: 'com.example.api.frontweb.client.model',
		dateLibrary: 'legacy',
		java8: 'true',
		library: 'webclient',
	]
    systemProperties = [
        modelDocs: 'false'
    ]
}
```

サーバ側の設定で重要なパラメータは generatorName、reactive、interfaceOnly の 3パラメータ。
上記例は WebFlux用のコードを生成するようにしている。
以下、参考までに SpringMVC用とWebFlux用のコードを生成する際の
パラメータを示す。

|設定名|SpringMVC|WebFlux|
|-|-|-|
|generatorName|spring|spring|
|reactive|false(default)|true|
|interfaceOnly|true|true|


クライアント側の設定で重要なパラメータは generatorName, library の 2パラメータ。
上記例は　WebClientを用いたコードを生成するようにしている。
以下、参考までに、RestTemplate と WebClient を用いたコードを生成する際の
パラメータを示す。

|設定名|RestTemplate|WebClient|
|-|-|-|
|generatorName|java|java|
|library|resttemplate|webclient|


generatorName などの第一階層のパラメータの詳細は以下を参照のこと。  
https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator-gradle-plugin#openapigenerate

configOptions のパラメータは、generatorName で指定した
generator 固有の設定となっている。
generatorName 毎の configOptions の設定の詳細は以下を参照のこと。  
https://github.com/OpenAPITools/openapi-generator/blob/master/docs/generators.md


## コード生成
前述のように `build.gradle` を修正した場合は、
Javaのコンパイル前にコード生成のタスクが実行されるように定義したため、
gradle でビルドすれば、ビルドの過程でコードが生成されます。

もちろん、定義したコード生成のタスクだけ gradle で実行すれば、
一つの OAS からコードが生成されます。
また、`compileJava` タスクを実行すれば、
すべての OAS からコードを生成した上で、コンパイルまで実行します。
