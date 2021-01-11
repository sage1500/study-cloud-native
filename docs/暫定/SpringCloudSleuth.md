Spring Cloud Sleuth
===

### memo

- Spring Cloud共通
  - dependencyManagement で Spring Cloud の BOM を取り込む

```groovy
buildscript {
    ext.versions = [
        'springCloud': 'Hoxton.SR8',
    ]
}

dependencies {
    // Sleuth
    implementation 'org.springframework.cloud:spring-cloud-starter-sleuth'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${versions.springCloud}"
    }
}

```

#### WebFlux

https://cloud.spring.io/spring-cloud-sleuth/2.0.x/multi/multi__integrations.html#_http_integration

15.5.4 WebFlux support
spring.sleuth.web.filter-order
要検証

15.6.3 WebClient
検証済

#### gRPC

```groovy
implementation 'io.zipkin.brave:brave-instrumentation-grpc'
```



