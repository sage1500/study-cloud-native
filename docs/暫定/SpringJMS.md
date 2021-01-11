Spring JMS
===

※リアクティブJMSというのもあるらしいが、いったん置いておく。

★TODO Amazon SQS の話も

Spring JMS with ActiveMQ
---

### JMS共通

#### 依存関係
以下の依存関係を追加する。
- `org.springframework:spring-jms`
- `jakarta.jms:jakarta.jms-api`
- 加えて、JSONのコンバートするために以下も必要
  - `com.fasterxml.jackson.core:jackson-databind`

例)  
```groovy
implementation 'org.springframework:spring-jms'
implementation 'jakarta.jms:jakarta.jms-api'
implementation 'com.fasterxml.jackson.core:jackson-databind'
```

### ActiveMQを使用する場合
#### 依存関係
以下の依存関係を追加する。

- `org.springframework.boot:spring-boot-starter-activemq`

なお、上記の依存関係にはJMS共通の3つの依存関係も含まれているため、ActiveMQ用の依存関係だけ書いても動作はする。

例)  
  ```groovy
  implementation 'org.springframework.boot:spring-boot-starter-activemq'
  ```

#### application.properties
`spring.activemq.broker-url` に ActiveMQの接続先を設定する。

例)  
```
spring.activemq.broker-url=tcp://localhost:61616
```

### Javaコード

#### Java Config
- 受信側に `@EnableJms` アノテーションを付与する。(送信側に合っても問題ないが、若干無駄かも？)(実は付与しなくても動くっポイ)  
  送受信ともに Java同士でよければこれだけで動作する。ただし、送受信対象のクラスは `Serializable` なものに限る。
- Java以外にも対応するためにメッセージをJSONにコンバートする。
  - そのために `MessageConverter` のBeanを用意する。
    - この際、Java同士でもメッセージのPOJOのクラスが異なる場合にも対応するために、`TypeIdMappings` を設定する必要あり。
      - Java同士でも JSON を経由することで、異なるPOJOクラスを利用できるという利点がある。

```Java
@Configuration
@EnableJms
public class JmsConfig {
    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        converter.setTypeIdMappings(jmsTypeIdMappings());
        return converter;
    }

    @Bean
    public Map<String, Class<?>> jmsTypeIdMappings() {
        Map<String, Class<?>> m = new HashMap<>();

        // Register TypeId Mappings
        m.put("Foo", Foo.class);

        return Collections.unmodifiableMap(m);
    }
}
```

※FQCN
```java
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
```


#### 送信側コード
`JmsTemplate` または、 `JmsMessagingTemplate` をインジェクションして利用する。

例)  
```Java
@Component
@RequiredArgsConstructor
public class Hoge {
    private final JmsTemplate jmsTemplate;

    public void send(Foo foo) {
        jmsTemplate.convertAndSend("foo", foo);
    }
}
```

上記では、やっていないが、必要に応じて、インタフェースと実装は分けた方がよい。


#### 受信側コード
受信メソッドに `@JmsListener` アノテーションを付与する。

Spring MVC と同様に、`@Header` アノテーションを付与した引数をつけることで、
ヘッダーのパラメータも取得できる。

例)  
```Java
@Component
@Slf4j
public class Receiver {
    @JmsListener(destination = "foo")
    public void receiveMessage(Foo foo, 
        @Header(JmsHeaders.MESSAGE_ID) String messageId) {
        log.info("★receive:MessageId:{} foo {}", messageId, foo);
    }
}
```

#### ログ設定
- ★TODO 受信処理のロギング設定（エラー時含む）
  - `@JmsListener` アノテーションが付いているメソッドに対して AOP でログを埋め込む
  - エラーハンドラは `DefaultJmsListenerContainerFactory` に設定した Bean定義する。
    `@JmsListener` アノテーションの `containerFactory`属性に、そのBean名を設定する（`jmsListenerContainerFactory`という名前でBean定義している場合は設定不要）。

#### 受信スレッド設定
- 並列度の全体設定は `spring.jms.listener.concurrency`, `spring.jms.listener.max-concurrency` で設定。
- `@JmsListener` アノテーションの `concurrency`属性で受信ハンドラメソッド個別に上書き設定設定可能
- スレッドの Executor を差し替えたい場合は、`DefaultJmsListenerContainerFactory` に設定した Bean定義する。
  `@JmsListener` アノテーションの `containerFactory`属性に、そのBean名を設定する（`jmsListenerContainerFactory`という名前でBean定義している場合は設定不要）。

#### ★TODO Broker の切り替え

#### ★その他：可視性タイムアウト、メッセージ受信待機時間、デッドレターキュー設定

