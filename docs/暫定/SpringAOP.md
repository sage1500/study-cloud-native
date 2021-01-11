Spring AOP
===

概要
---

### Spring AOP の機能
※参考文献抜粋

> - Spring AOP は、純粋な Java で実装されています。特別なコンパイルプロセスは必要ありません。
> - Spring AOP は現在、メソッド実行の結合点（Spring Bean でのメソッドの実行をアドバイスする）のみをサポートしています。

※重要：
JoinPoint は、Spring に管理されている Bean のメソッド実行のみが対象。


### アドバイス
※参考文献抜粋

> Spring AOP には、次の種類のアドバイスが含まれています。
> - Before アドバイス : ジョインポイントの前に実行されるが、例外がスローされない限り、実行フローがジョインポイントに進むことを防ぐ機能がないアドバイス。
> - After returning アドバイス : ジョインポイントが正常に完了した後に実行するアドバイス（たとえば、メソッドが例外をスローせずに戻る場合）。
> - After throwing アドバイス : 例外をスローしてメソッドが終了した場合に実行されるアドバイス。
> - After (finally) アドバイス : ジョインポイントが存在する方法（通常または例外的なリターン）に関係なく実行されるアドバイス。
> - Around アドバイス : メソッド呼び出しなどのジョインポイントを囲むアドバイス。これは最も強力なアドバイスです。Around アドバイスは、メソッド呼び出しの前後にカスタム動作を実行できます。また、ジョインポイントに進むか、独自の戻り値を返すか例外をスローすることにより、推奨されるメソッド実行をショートカットするかを選択する責任もあります。


実装
---

### 依存関係
依存関係に以下を追加する。
- org.springframework.boot:spring-boot-starter-aop

例)  
```groovy
implementation "org.springframework.boot:spring-boot-starter-aop"
```

### Aspect

Aspect を定義しているクラスに `@Aspect` アノテーションを付与する。
ComponentScan に引っかかるように `@Component` アノテーションなどを付与する。

例)  
```Java
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class DemoAspect {
    // ...
}
```

### アドバイスの引数
※詳細は参考文献参照

Aroundアドバイスは、引数に `ProceedingJoinPoint` を取る。
Aroundアドバイス以外は、引数は必須ではないが、必要であれば、`JoinPoint`(FQCN=`org.aspectj.lang.JoinPoint`) を取ることができる。

例)  
```Java
@Before("anyRestController() && anyGetMappingMethod()")
public void doBefore() {
    // 前処理
    log.info("★doBefore");
}

@After("anyRestController() && anyGetMappingMethod()")
public void doAfter(JoinPoint jp) {
    // 後処理
    log.info("★doAfter {}", jp.getSignature().toShortString());
}
```

例)  
```Java
@Around("anyRestController() && anyGetMappingMethod()")
public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
    // 前処理
    log.info("★doAround start {}", pjp.getSignature().toShortString());

    // 元の処理
    Object retVal = pjp.proceed();

    // 後処理
    log.info("★doAround end");
    return retVal;
}
```

### ポイントカット式
※詳細は参考文献参照

利用しそうなポイントカット式の例。

- `within` : 指定したパッケージ配下
    - `within(com.example..*)`
- `@within` : 指定したアノテーションを付与されたクラス
    - `@within(org.springframework.web.bind.annotation.RestController)`
- `@annotation` : 指定したアノテーションを付与されたメソッド
    - `@annotation(org.springframework.web.bind.annotation.GetMapping)`
- `bean` : 指定した名前のBean
    - `bean(*Controller)`
- `execution` : 指定したメソッド
    - `execution(public * *(..))`
    - `execution(* *..*.*Controller.*(..))`
    - `execution(* com..*.*(..))`
    - `execution(* *..*.*.hello(..))`

### 実装例

```Java
package com.example.demo;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Aspect
@Slf4j
public class DemoAspect {
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void anyRestController() {
    }

    @Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping)")
    public void anyGetMappingMethod() {
    }

    @Before("anyRestController() && anyGetMappingMethod()")
    public void doBefore(JoinPoint jp) {
        // 前処理
        log.info("★doBefore {}", jp.getSignature().toShortString());
    }

    @After("anyRestController() && anyGetMappingMethod()")
    public void doAfter(JoinPoint jp) {
        // 後処理
        log.info("★doAfter {}", jp.getSignature().toShortString());
    }

    @Around("anyRestController() && anyGetMappingMethod()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        // 前処理
        log.info("★doAround start {}", pjp.getSignature().toShortString());

        // 元の処理
        Object retVal = pjp.proceed();

        // 後処理
        log.info("★doAround end");
        return retVal;
    }
}
```

### 参考文献

- https://spring.pleiades.io/spring-framework/docs/5.2.9.RELEASE/spring-framework-reference/core.html#aop
- https://qiita.com/daisuzz/items/de937816a5d7c9210469
