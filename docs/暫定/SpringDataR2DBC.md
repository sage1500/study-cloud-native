Spring Data R2DBC
===

- Java側でIDを振る実装時のポイント
    - 楽観ロック用の version カラムがあることがほぼ前提。ないと面倒。
    - 更新の場合
        - findXxx() した後に save() ※IDプロパティに値が設定されており、versionプロパティが 1以上の値
    - 新規追加の場合
        - ID用のプロパティをIDを設定
        - versionカラムは未設定(0を設定)


## その他 Spring Data シリーズメモ

### Spring Data JDBC

[派生削除クエリ](https://spring.pleiades.io/spring-data/jdbc/docs/current/reference/html/#repositories.core-concepts)を
まだサポートしていない。

参照：
https://github.com/spring-projects/spring-data-jdbc/issues/771


加えて、delete に失敗しても、例外がスローされない。
そのため、削除できてかどうかを判定しようと思うならば、
現時点では、Spring Data JDBC は使えない。

### Spring Data JPA

- Entity に `javax.persistence.Entity` アノテーションの付与が必要
- IDカラムのフィールドに `@Id`(`javax.persistence.Id`) アノテーションの付与が必要  
  ※Spring Data 標準の `@Id`(`org.springframework.data.annotation.Id`) アノテーション ではダメ。

