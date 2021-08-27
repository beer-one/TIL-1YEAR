# Reactor

https://projectreactor.io/

회사에서 Spring Webflux 기반으로 HTTP API 서버를 개발하는데 주요 개발 스택인 Reactor에 대한 지식을 넓히고자 공식 문서를 보면서 공부한 내용을 정리해보겠다.



## Reactor 소개

Reactor는 JVM 환경에서 동작하는 non-blocking reactive 라이브러리이다. Reactor에서는 두 가지의 비동기 시퀀스 API를 제공하는데 `Mono`(0 또는 1)와 `Flux`(N개 요소)가 있다. Reactor에서는 또한 `reactor-netty` 프로젝트와 non-blocking 상호 프로세스 통신을 지원한다. MSA에 적합한 Reactor Netty는 HTTP, TCP, UDP를 위한 backpressure가 준비된 네트워크 엔진을 제공한다.

Reactor는 Java 8 이상에서 사용이 가능하다. Reactor가 Java8 함수형 API(CompletableFuture, Stream, Duration)와 관련이 있기 때문일 것이다. 



Reactor 라이브러리를 사용하기 위해서는 아래와 같이 gradle 설정을 해준다. (gradle 5.0:arrow_up:)

```groovy
plugins {
    id "io.spring.dependency-management" version "1.0.9.RELEASE"	
}

dependencies {
    implementation platform('io.projectreactor:reactor-bom:2020.0.10')
    implementation 'io.projectreactor:reactor-core' 
}
```

만약 Kotlin을 사용하고 있다면 아래와 같이 해준다. (KotlinDSL)

```groovy
plugins {
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
}

dependencies {
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
}
```



## Reactive Programming

Reactor는 Reactive Programming을 할 수 있게 만들어진 라이브러리인데, Reactor를 사용하기 전에 Reactive Programming의 개념을 대충이라도 알아야 할 것이다.

Reactive Programming은 **데이터 스트림**과 **변화의 전파(propagation)**와 관련된 비동기 프로그래밍 패러다임이다. 이는 프로그래밍 언어를 통해 쉽게 정적이나 동적인 데이터 스트림을 표현할 수 있다는 것을 의미한다. 

Reactive Programming 패러다임은 객체지향 언어에서는 보통 옵저버 패턴으로 표현된다. 보통은 Reactive Stream 패턴을 Iterator 패턴과 유사하다고 생각할 수 있는데, Iterator 패턴과 Reactive Stream의 차이점은 Iterator는 Pull-based이고 Reactive Stream은 Push-based이다. 

Reactive Stream에서는 Publisher와 Subscriber가 있는데, 새로 사용한 값이 올 때 Publisher는 Subscriber에게 알려줄 수 있으며, 이렇게 Publisher가 push를 해주는 것은 Reactive의 핵심이다. 그리고, 푸쉬된 값을 적용시키는 연산자를 명령형 대신 선언형으로 표현한다.

그리고 값을 푸쉬하는 것 외에도 에러 핸들링과 완료했을 때에 대한 핸들링도 정의할 수 있다. Publisher는 Subscriber에게 새로운 값을 푸쉬할 수 있지만, 에러를 보내거나 (더 이상 보낼 것이 없을 때)완료 신호를 보낼 수도 있다. 에러나 완료 신호를 받는다면 시퀀스는 종료된다. 이런 특성 때문에 값이 없거나(바로 완료 신호를 줌), 하나의 값(하나 보내고 완료 신호)이거나 유한개의 값이거나(여러 개 보내고 완료 신호) 또는 무한히 많은 값(완료 신호를 주지 않는 경우)을 표현할 때 아주 유연하게 표현할 수 있다.







































