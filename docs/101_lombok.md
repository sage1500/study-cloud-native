# Lombok

## Lombok とは

TODO Webで調べましょう。

本家: [Project Lombok](https://projectlombok.org/)


## IDE向けのインストール

各IDE向けのセットアップ方法は本家のページに書いてあるので、
それを参考にセットアップする。

セットアップ方法: [Using lombok](https://projectlombok.org/setup/overview)


## Maven, Gradle での対応方法

Maven や、Gradle のビルドファイルに Lombok の設定を組み込む方法も
IDE向けのインストール方法と同じ場所の記載されている。

ただ、通常は、 [Spring Initializr](https://start.spring.io/) で
Dependenies に Lombok を追加した状態で、
プロジェクトを生成すれば自動で Maven や Gradle のビルドファイルに
Lombok の設定が組み込まれるため、あまり気にしなくてよい。


## 今回の主要なアノテーション

Lombok で定義されているアノテーションはいくつもあるが、
特に今回のサンプルアプリケーション実装で有用なものとして多用しているのは、
以下のアノテーションである。

- `@Data`  
    定義したフィールドのセッターおよびゲッターに加え、`toString()`、`equals()`、`hashCode()` を実装してくれる。  
    Form や Entity などの実装に便利。
- `@RequiredArgsConstructor`  
    初期値を設定していない final なフィールドに対して、値を初期化するコンストラクタを実装してくれる。  
    Spring Framework では、Bean を引数にもつコンストラクタがあると、コンストラクタインジェクションしてくれる。そのため、Controller などの Spring Framework の Bean の実装に便利。
- `@Slf4j`  
    `log` というフィールド名でロガーを設定してくれる。  
    便利。

## 使用例

### Form の実装

`@Data`を使用して実装した例。

```java
@Data
public class TodoForm implements Serializable {
    private static final long serialVersionUID = 1L;

    private String todoId;
    private String todoTitle;
    private long version;
}
```

### `Controller`の実装

`@RequiredArgsConstructor` と `@Slf4j` を使用して実装した例。

```java
@Controller
@RequestMapping("/todo/create")
@RequiredArgsConstructor
@Slf4j
public class TodoCreateController {
    private final Mapper dozerMapper;

    // ...

    @PostMapping("execute")
    public Mono<Rendering> execute(TodoForm todoForm) {
        log.debug("[TODO-CREATE]execute: {}", todoForm);

        // TodoForm を TodoResource に変換
        TodoResource todo = dozerMapper.map(todoForm, TodoResource.class);
        log.debug("[TODO-CREATE]execute : resource={}", todo);

        // ...
    }

    // ...
}
```