

公開されている Docker イメージを利用する。

参考：
https://hub.docker.com/r/jboss/keycloak

上記ページの記載内容を参考にする。


以下の docker-compose.yml を用意し、
docker-compose で起動する。

```
version: '3'

services:
  db:
    image: postgres:13-alpine
    environment:
      POSTGRES_PASSWORD: keycloak
      POSTGRES_USER: keycloak
      POSTGRES_DB: keycloak
    volumes:
      - ../_data/keycloak/db:/var/lib/postgresql/data
  keycloak:
    image: jboss/keycloak
    environment:
      DB_VENDOR: postgres
      DB_ADDR: db
      DB_USER: keycloak
      DB_PASSWORD: keycloak
      KEYCLOAK_USER: keycloak
      KEYCLOAK_PASSWORD: keycloak
    ports:
      - 18080:8080
      - 18443:8443
```

初回はDBの初期化の都合で失敗するかもしれない。
失敗したら一度 docker-compose で down して、up しなおす。


docker-compose up したら
起動したらブラウザで localhost:18080 にアクセスする。

`Administration Console` にリンクをクリック。
ユーザIDとパスワードを聞かれたら keycloak/keycloak を入力。


## 表示ラベルの国際化

Master レルムの

Realm Settings → Themes 
  Internationalization Enabled: ON
  Default Locale: ja
  
 Saveボタン押下

一度、サインイン時に言語が選べるようになる。

以降は、言語に日本語を選んだ場合を記載


## デモ用の情報作成

## レルム作成
master レルムをそのまま使ってもよいけど、デモ用にレルムを作成する。

左上の Master （これが現在のレルム名）にカーソルを合わせて、
「レルムの追加」ボタン をクリック。

名前: ＜レルム名＞
有効: ON

「作成」ボタンをクリック

レルムの設定 → テーマ
  国際化の有効: オン
  デフォルト・ロケール: ja
  
　「保存」ボタン押下

## ロール作成
今回はユーザのロールによる認証はしないので作らないくてもよいけど。

## ユーザ作成

ユーザ
　「ユーザの追加」ボタンを押下
　　ユーザー名: <任意のユーザ名>
　　ユーザの有効: オン
  
　「保存」ボタン押下

※作りたいユーザ分だけ実施

ユーザのパスワード設定

「クレデンシャル」タブ
　パスワード: <任意のパスワード>
　新しいパスワード（確認）: <上記パスワード>
　一時的: オフ  # オンでも構わないが
 
　「パスワードを設定」ボタン押下

ロールの設定
　今回はロールによる認証はしないので設定しなくてもよいけど。

## クライアントスコープ作成
今回はスコープによる認証はしないので作らなくてもよいけど。

## クライアント作成

クライアント
　「作成」ボタン押下
　　クライアントID: <クライアントの名前>
　　クライアント・プロトコル: openid-connect
    
　　「保存」ボタン押下

「設定」タブ
　アクセスタイプ: confidential # 今回は試すのはパブリッククライアントではなく、コンフィデンシャルクライアントなので
　有効なリダイレクトURI:
　　http://localhost:8080/login/oauth2/code/*
　　http://localhost:8080/

　※実際に動かしてみて、「無効なリダイレクトURIです。」と表示されたら、URL中にあるリダイレクトURIを登録する。

　「保存」ボタン押下
