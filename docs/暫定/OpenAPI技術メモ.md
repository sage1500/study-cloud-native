OpenAPI技術メモ
===

Gradle による Specファイルからのコード自動生成
---

★書きかけ

### 概要
Gradle による Specファイルからのコード自動生成は、
[OpenAPI Generator Gradle Plugin](https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator-gradle-plugin) を使用することで実現できます。
このプラグインは、[OpenAPI Generator](https://github.com/OpenAPITools/openapi-generator/) のラッパーです。
そのため、Gradle による Specファイルからのコードの自動生成方法を理解するには、最終的に、OpenAPI Generator に与えるパラメータと、OpenAPI Generator Gradle Plugin の記載方法を知る必要があります。

### ジェネレータ
OpenAPI Generator は、複数のプログラミング言語用のコードを生成でき、コード生成時に、その言語用にコード生成するか、各言語用のジェネレータを指定します。
しかし、Javaの場合は、より複雑で、
たとえば、Spring framework など、どのフレームワークに適したコードを生成するかでジェネレータが分かれています。
そして、コード生成用のパラメータはジェネレータごとに異なります。
そのため、どのようなジェネレータがあり、各ジェネレータごとにどのようなパラメータがあるか調べる必要があります。

### 調査のためのヒント

- OpenAPI Generator Gradle Plugin の Gradle のビルドファイルでどのように記述すればよいか
  - https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator-gradle-plugin
- OpenAPI Generator のジェネレータの一覧とパラメータ
  - https://github.com/OpenAPITools/openapi-generator/tree/master/docs/generators

- openApiGenerate のパラメータ
    - generatorName
    - outputDir
    - inputSpec
    - configFile
    - modelNamePrefix
    - modelNameSuffix
    - instantiationTypes
    - typeMappings
    - importMappings
    - configOptions : configFile で指定した json ファイルに外だし可能

- generatorName が java の場合、以下のパラメータは設定するかも
    - booleanGetterPrefix : デフォルトが get なので、is に変更する
    - dateLibrary
    - java8 : デフォルトが true なので、多分、そのまま
    - library : Restのクライアントライブラリを設定。resttemplate, webclient あたりを設定する
    - serializableModel : デフォルト false
    - useBeanValidation : デフォルトが false になっているが？・・・
- generatorName が spring の場合、以下のパラメータを設定するかも
    - apiPackage
    - basePackage
    - booleanGetterPrefix
    - configPackage
    - dateLibrary
    - interfaceOnly
    - invokerPackage
    - java8
    - library
    - modelPackage
    - reactive
    - serializableModel
    - unhandledException : デフォルト false
    - useBeanValidation : デフォルト true

### サンプル

```Gradle
plugins {
	// ...

    // ADD
    id "org.openapi.generator" version "4.2.1"
}

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

// OAS の検証
// ※なくてもよいが、
openApiValidate {
    inputSpec = "$rootDir/specs/todo-v3.0.yml"
}

// コード生成タスク
openApiGenerate {
    generatorName = "spring"
    configFile = "$rootDir/specs/generator-config-spring.json"
    inputSpec = "$rootDir/specs/todo-v3.0.yml"
    outputDir = "$buildDir/generated"
    configOptions = [
        dateLibrary: "java8"
    ]
    systemProperties = [
        modelDocs: 'false'
    ]
}

// コード生成対象が２つ以上ある場合は、以下のような感じで追加で書けばよい。
task buildJavaClient(type: org.openapitools.generator.gradle.plugin.tasks.GenerateTask){
    generatorName = "java"
    configFile = "$rootDir/specs/generator-config-java-client.json"
    inputSpec = "$rootDir/specs/todo-v3.0.yml"
    outputDir = "$buildDir/generated"
    systemProperties = [
        modelDocs: 'false'
    ]
}

// compileJava のタスク実行前に openApiValidate, openApiGenerate タスクを実行する。
compileJava.dependsOn tasks.openApiValidate, tasks.openApiGenerate

// 生成したコードの出力先をソースフォルダに設定（コンパイル対象にするために）
sourceSets.main.java.srcDir "${openApiGenerate.outputDir.get()}/src/main/java"
sourceSets.main.resources.srcDir "${openApiGenerate.outputDir.get()}/src/main/resources"
```

`configFile` に書く内容の大部分は、`openApiGenerate` タスク中に直接書くこともできる。
好みの問題。


### メモ

ジェネレータの `java` と `spring` で生成するモデルのコードが違う。`spring` の方がバリデータが必要なのだが、バリデータ用のアノテーションが付いていないような？？

### 設定

サーバの設定

|設定名|SpringMVC|WebFlux|
|-|-|-|
|generatorName|spring|spring|
|reactive|false(default)|true|
|interfaceOnly|true|true|


クライアントの設定

|設定名|RestTemplate|WebClient|
|-|-|-|
|generatorName|java|java|
|library|resttemplate|webclient|


例）サーバ側(WebFlux)用のコード生成

- gradle  
  ※必ずそうしなければならないわけではないが、以下の例では、細かい設定は config.json に外だししている。
    ```groovy
    openApiGenerate {
        generatorName = "spring"
        configFile = "$projectDir/src/main/specs/config/config.json"
        inputSpec = "$rootDir/apispec/specs/todo-v3.0.yml"
        outputDir = "$buildDir/generated"
        systemProperties = [
            modelDocs: 'false'
        ]
    }
    ```
- config.json  
    ```json
    {
        "apiPackage": "com.example.api.todo.server.api",
        "invokerPackage": "com.example.api.todo.server.invoker",
        "modelPackage": "com.example.api.todo.server.model",
        "dateLibrary": "legacy",
        "java8": "true",
        "reactive": "true",
        "interfaceOnly": true
    }
    ```