WebFlux
===

## はじめに

WebFlux を用いると
ノンブロッキングなWebアプリケーションを構築できます。

たとえば、ある要求に対して、RESTで他のサーバにアクセスする処理があるとします。

従来のブロッキングされる方式では、他のサーバからの応答があるまで、要求を処理しているスレッドは待たされます（ブロッキングされます）。そのため、大量の要求が同時に発生すると、大量のスレッドが必要になります。待っているだけで何もしていないにもかかわらずです。

ノンブロッキングな方式では、他のサーバに要求を送信した後は、
そのスレッドは解放され、別の用途に使用されます。他サーバからの応答を受信したタイミングで、改めてスレッドが割り当てられます。ただ、待っているだけの時はスレッドが割り当てられないため、特に大量の要求が同時に発生した場合に使用するスレッドの数を減らすことができます。


## ドキュメント（本家）
- Web Flux リアクティブスタック  
    https://spring.pleiades.io/spring-framework/docs/current/reference/html/web-reactive.html
- Reactor  
    https://projectreactor.io/docs/core/release/reference/

## Spring MVC との実装上の違い（概要）<a id="diff"></a>

Spring MVCとの大まかな違いは以下のとおり。

- コントローラー
    - Spring MVC のコードは大部分は WebFlux でも流用可能
    - WebFlux が追加でやれること
        - 返値の型を `Mono` や `Flux` でラップすることができる。  
          →RESTなどノンブロッキングが必要なタイミングで別途解説
    - WebFlux がやれないこと
        - POST時は params でのハンドラメソッドのマッピングは利用できない。
        - `RedirectAttributes` が利用しない。
- フィルター
    - `Fiter` ではなく、`WebFilter` で実装する。  
        →横断的にログを取得するタイミングで別途解説
- セキュリティ
    - Spring Security の設定方法が違う。  
        →認証・認可のタイミングで別途解説

## 依存関係

Spring MVC の代わりに、WebFlux を利用するには、
`spring-boot-starter-web`の代わりに、`spring-boot-starter-webflux`を依存関係に加えます。

```groovy
// implementation 'org.springframework.boot:spring-boot-starter-web'
implementation 'org.springframework.boot:spring-boot-starter-webflux'
```

なお、[Spring Initializr](https://start.spring.io/)では、
「Spring Web」の代わりに、「Spring Reactive Web」を選択します。

## 実装
[Spring MVC との実装上の違い（概要）](#diff)で示したように
一旦、以下の点に気をつけておけば特に問題なし。

- POST時は params でのハンドラメソッドのマッピングは利用できない。
- `RedirectAttributes` が利用しない。

フィルターやセキュリティについては、それが必要になったタイミングで解説する。
