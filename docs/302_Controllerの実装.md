# Controllerの実装

基本的には
[TERASOLUNAガイドラインの記載](http://terasolunaorg.github.io/guideline/5.6.1.RELEASE/ja/ImplementationAtEachLayer/ApplicationLayer.html#controller)
のとおり。

差分は以下のとおり。

- フィールドインジェクションではなく、コンストラクタインジェクションを利用する。  
    ※必須ではないが、Spring では、コンストラクタインジェクションを推奨しているらしいため。  
    ※TODO ひとまず、コンストラクタインインジェクションの実装方法は [lombok](101_lombok.md) に記載の部分を部分を参照。 
- POST時は params でのハンドラメソッドのマッピングは利用しない。  
    ※WebFlux で利用できないため。
- `RedirectAttributes` を利用しない。  
    ※WebFlux で利用できないため。何かしらの代替手段で実装する必要あり。  
    ※TODO 代替手段について



