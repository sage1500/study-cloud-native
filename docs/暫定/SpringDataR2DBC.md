Spring Data R2DBC
===

- Java側でIDを振る実装時のポイント
    - 楽観ロック用の version カラムがあることがほぼ前提。ないと面倒。
    - 更新の場合
        - findXxx() した後に save() ※IDプロパティに値が設定されており、versionプロパティが 1以上の値
    - 新規追加の場合
        - ID用のプロパティをIDを設定
        - versionカラムは未設定(0を設定)
