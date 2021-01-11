SpringBoot一般
===

ドキュメント
---

https://spring.io/
https://spring.pleiades.io/

https://docs.spring.io/spring-boot/docs/current/reference/html/
https://spring.pleiades.io/spring-boot/docs/current/reference/html/

特定のバージョンのドキュメントを読みたい場合は `current` の部分をバージョンに変える。

日本語訳の原文を読みたい場合は、
`spring.pleiades.io` を `docs.spring.io` に変える。



DI
---

### コンストラクタ・インジェクション

lombok 使うと比較的簡単。
インジェクションしたいフィールドを `final` にして、
クラスに `@RequiredArgsConstructor` アノテーションをつければよい。

    ```Java
    @Component
    @RequiredArgsConstructor
    public class FooComponent {
        private final BarComponent barComponent;

        // ...
    }
    ```

`@Autowired` とか、`@Inject` とか書かなくてよいので楽。

### コンストラクタ・インジェクションで名前指定したい場合

名前指定でインジェクションしたい場合は、以下のいずれかで対応する。
- フィールド名をコンポーネント名に合わせる。
  - インジェクション元のクラスのインスタンスが1つしかない場合は、名前指定は無視される。  
    どうしてもその名前のコンポーネントだけをインジェクションし、そうでない場合はAP起動失敗でもよいという場合には、
    この方法は使えない。    
- `@RequiredArgsConstructor` をあきらめて、コンストラクタを書き、対象の引数に `@Qualifier`(`@Named`でもよいはず)をつける。

    ```Java
    @Component
    public class FooComponent {
        private final BarComponent barComponent;
        public FooComponent(@Qualifier("hogeComponent") BarComponent barComponent) {
            this.barComponent = barComponent;
        }

        // ...
    }
    ```
