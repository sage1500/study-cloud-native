
dockerhub にある Dockerイメージを利用する。

参考：
https://hub.docker.com/r/webcenter/activemq/

上記ページの記載内容を参考にする。


以下の docker-compose.yml を用意し、
docker-compose で起動する。

```
version: '3'

services:
  activemq:
    image: webcenter/activemq:latest
    ports:
      - 8161:8161
      - 61616:61616
      - 61613:61613
    environment:
      ACTIVEMQ_NAME: amq
      ACTIVEMQ_REMOVE_DEFAULT_ACCOUNT: 'True'
      ACTIVEMQ_ADMIN_LOGIN: admin
      ACTIVEMQ_ADMIN_PASSWORD: admin
    volumes:
      - ../_data/activemq/data:/data/activemq
      - ../_data/activemq/log:/var/log/activemq
```


以下のURLにブラウザでアクセスすると管理画面が見える(ユーザID/パスワードは admin/admin)
http://localhost:8161/admin/


