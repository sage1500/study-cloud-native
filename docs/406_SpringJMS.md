Spring JMS
===

Java Message Service(JMS) を利用することで、
複数の Message Service を同じ Javaコードで扱うことができるので便利。
扱える Message Service としては以下のようなものがある。

- Apache ActiveMQ
- Azure Service Bus
- Amazon SQS

ここでは、ActiveMQ を用いた正常系のみの簡単な実装を紹介する。
異常系を含めた詳細は
Macchinetta の以下のページを参考にしてほしい。

https://macchinetta.github.io/cloud-guideline/current/ja/ImplementationAtEachLayer/Queuing/index.html

## JMS共通

### 依存関係

以下の依存関係を追加する。
- `org.springframework:spring-jms`
- `jakarta.jms:jakarta.jms-api`
- 加えて、JSONのコンバートするために以下も必要
  - `com.fasterxml.jackson.core:jackson-databind`

```groovy
implementation 'org.springframework:spring-jms'
implementation 'jakarta.jms:jakarta.jms-api'
implementation 'com.fasterxml.jackson.core:jackson-databind'
```

## ActiveMQを使用する場合
### 依存関係
以下の依存関係を追加する。

- `org.springframework.boot:spring-boot-starter-activemq`  

なお、上記の依存関係にはJMS共通の3つの依存関係も含まれているため、ActiveMQ用の依存関係だけ書いても動作はする。

```groovy
implementation 'org.springframework.boot:spring-boot-starter-activemq'
```

### `application.properties`

`spring.activemq.broker-url` に ActiveMQの接続先を設定する。

```
spring.activemq.broker-url=tcp://localhost:61616
```

## Javaコード
### Java Config
- 受信側に `@EnableJms` アノテーションを付与する。(送信側に合っても問題ないが、若干無駄かも？)(実は付与しなくても動くっポイ)  
  ※送受信ともに Java同士でよければこれだけで動作する。ただし、送受信対象のクラスは `Serializable` なものに限る。
- Java以外にも対応するためにメッセージをJSONにコンバートする。
  - そのために `MessageConverter` のBeanを用意する（Bean名は`jacksonJmsMessageConverter`にする）。
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

※別パッケージに同じクラス名のものがあるのでFQCNに注意。
```java
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
```


### 送信側コード
`JmsTemplate` または、 `JmsMessagingTemplate` を　DI して利用する。

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

上記 `jmsTemplate.convertAndSend("foo", foo)` の第一引数 `"foo"` は宛先の名前。


### 受信側コード

- 受信メソッドに `@JmsListener` アノテーションを付与する。
    - `destination` で送信側から見た宛先の名前を指定する。
- Spring MVC と同様に、`@Header` アノテーションを付与した引数をつけることで、ヘッダーのパラメータも取得できる。

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
