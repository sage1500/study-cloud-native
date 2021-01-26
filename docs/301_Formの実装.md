# Formの実装

基本的には
[TERASOLUNAガイドラインの記載](http://terasolunaorg.github.io/guideline/5.6.1.RELEASE/ja/ImplementationAtEachLayer/ApplicationLayer.html#formobject)
のとおり。

ただし、ここでは、Lombok を使用するため、
クラスに `@Data` アノテーションを付与し、
セッターおよびゲッターは自前で実装しない。

```java
@Data
public class SampleForm implements Serializable {
    private String id;
    private String name;
    private Integer age;
    private String genderCode;
    private Date birthDate;
}
```
