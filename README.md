# study-cloud-native

## デモアプリ
### 環境構築

1. 各ミドルウェア起動  
    `docker\up-all.bat` を実行する。  
      
    以下、参考情報。  
    上記のコマンドは、以下のディレクトリ配下の `docker-compose.yml` を `docker-compose up -d` で
    それぞれ起動している。
    そのため、`docker\up-all.bat` を実行する代わりに、個別に `docker-compose up -d` を実行してもよい。
    - docker/
        - scndb/
        - scnkeycloak/
        - scnredis/
        - scnactivemq/
1. db のスキーマ設定  
    - スキーマのファイル: `modules\todo\src\main\resources\schema-postgresql.sql`
    - psql を実行するバッチファイル: `docker\scndb\db-cli.bat`  
      ※実行時にパスワードを聞かれたら postgres と入力

### 実行

4つのコンソールから以下のコマンドをそれぞれ実行する。

```
./gradlew :modules:notifier:bootRun
./gradlew :modules:todo:bootRun
./gradlew :modules:hello:bootRun
./gradlew :modules:frontweb:bootRun
```

### 動作確認
#### 画面から確認

- `http://localhost:8080/` にアクセスする。  
    ユーザのID/PWは以下のものから選ぶ
    - user1/user1
    - user2/user2
- 適当に操作して、TODOをいくつか表示してみる。
- 以下を確認してみる。
    - ホーム画面に、`Hello <ユーザID>` と TODOの一覧が表示されること。
    - TODOを追加すると、`notifier` のコンソールにログが出力されること。

#### ヘルスチェック確認

以下のコマンドをそれぞれ投入し、`{"status":"UP"}` が返ってくることを確認する。

```
> curl.exe -v http://localhost:8080/manage/health
> curl.exe -v http://localhost:8081/manage/health
> curl.exe -v http://localhost:8082/manage/health
> curl.exe -v http://localhost:8083/manage/health
```

以下のディレクトリ配下の `docker-compose.yml` を `docker-compose down` を実行し、
各コンテナを落とした後に上記コマンドを実行するとどうなるか確認する。

- docker/
    - scndb/
    - scnkeycloak/
    - scnredis/
    - scnactivemq/

### TODO(デモアプリ)
- 環境
    - PostgreSQL のスキーマは自動で作成するように
- デモアプリ
    - クラウドネイティブ・アプリケーションと直接絡まない部分
        - バリデーション
        - メッセージ外部化
    - dockerイメージ作成をビルドファイルに組み込み


### 参考

#### KeyCloak の設定

KeyCloak の設定は作成手順。

- 管理画面: `http://localhost:18080/auth/admin/` ※ID/PW=keycloak/keycloak
- 以下を作成
    - レルム: demo
    - クライアント:
        - 名前: demoapp
        - アクセスタイプ: confidential
        - 有効なリダイレクトURI
            - `http://localhost:8080/login/oauth2/code/*`
            - `http://localhost:8080/`
        - シークレットの値をメモして置き、以下に反映
            - ファイル: `modules\frontweb\src\main\resources\application.properties`
            - プロパティ: `spring.security.oauth2.client.registration.keycloak.client-secret`
    - ユーザ: 以下の2ユーザ(ID/PW)を作成する。
        - user1/user1
        - user2/user2
