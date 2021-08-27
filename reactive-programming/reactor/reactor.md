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

Reactive Stream에서는 Publisher와 Subscriber가 있는데, 새로 사용한 값이 올 때 Publisher는 Subscriber에게 알려줄 수 있으며, 이렇게 Publisher가 push를 해주는 것은 Reactive의 핵심이다. 그리고, 푸쉬된 값을 적용시키는 연산자를 명령형 대신 선언형으로 표현한다. 그리고 값을 푸쉬하는 것 외에도 에러 핸들링과 완료했을 때에 대한 핸들링도 정의할 수 있다. Publisher는 Subscriber에게 새로운 값을 푸쉬할 수 있지만, 에러를 보내거나 (더 이상 보낼 것이 없을 때)완료 신호를 보낼 수도 있다. 에러나 완료 신호를 받는다면 시퀀스는 종료된다. 이런 특성 때문에 값이 없거나(바로 완료 신호를 줌), 하나의 값(하나 보내고 완료 신호)이거나 유한개의 값이거나(여러 개 보내고 완료 신호) 또는 무한히 많은 값(완료 신호를 주지 않는 경우)을 표현할 때 아주 유연하게 표현할 수 있다.



## Blocking code

요즘 애플리케이션은 아주 많은 동시 접속자를 감당해야 하는데, 하드웨어의 성능은 많이 발전했지만 소프트웨어에서의 성능은 아직 고민거리가 많다. 프로그램의 성능을 올리기 위한 대표적인 방법으로는 두 가지가 있는데 아래와 같다.

* 병렬 프로그래밍으로 더 많은 쓰레드와 더 많은 하드웨어 자원을 사용한다.
* 지금 사용하는 자원의 사용 방식에서 더 효율적인 방법을 찾는다.



보통, 자바로 개발하면 Blocking code를 사용하여 프로그램을 작성한다. Blocking code로 작성하는 방식은 병목현상이 발생하기 전 까지는 크게 문제는 없다. 하지만 병목현상이 발생하면 Blocking code를 실행하는 쓰레드를 여러개 추가하는 방법이 있는데, 리소스를 확장하는 것은 경합이나 동시성 문제를 야기할 수도 있다. 

심지어 Blocking 코드는 리소스를 낭비한다. 예를 들어, 프로그램에서 DB나 네트워크 호출과 같은 I/O가 발생하면 요청을 한 쓰레드에서 대기시간이 발생하는데, 이 때는 Idel 상태로 유지되어서 데이터를 기다리기 때문에 리소스가 낭비된다.

따라서 병렬 프로그래밍은 하드웨어의 많은 자원을 사용하여 성능을 내려고 하지만, 복잡도가 증가하며 자원을 낭비시키는 구간도 있기 때문에 성능을 개선시키는 완벽한 방법은 아니다.





## Asynchronicity to the Rescue

이 부분은 프로그램의 성능을 올리기 위한 두 번 째 방법인 `지금 사용하는 자원의 사용 방식에서 더 효율적인 방법을 찾는다.` 와 관련된 부분이다.

Blocking code와 추가 쓰레드를 도입하는 병렬 프로그래밍에서 문제되는 점은 I/O가 발생하면 쓰레드에서 대기시간이 발생하여 자원을 낭비하는 구간이 생긴다는 점이다. 그런데 비동기이면서 `Non-blocking` 코드를 작성하면 I/O가 발생하면 리소스를 사용하는 다른 작업으로 실행을 전환하고, 나중에 비동기 처리가 완료되면 현재 프로세스로 돌아오도록 할 수 있다. (Idel 상태에 대한 자원 낭비 축소, 그러면 I/O가 많은 애플리케이션에 reactive를 도입하면 이득을 좀 볼까?)







Java에서는 비동기 프로그래밍을 지원하기 위해 두 가지 모델을 제공한다.

* Callback: 비동기 메서드들은 반환값을 가지지 않지만 메서드의 결과 값을 사용할 수 있을 때 그 결과값을 사용하는 callback 파라미터를 가진다. 
* Futures: 비동기 함수는 즉시 `Future<T>` 를 반환한다. 비동기 프로세스는 T 값을 계산하지만 Future 객체가 그 값에 대한 접근을 래핑한다. 그 값은 즉시 사용 가능하지는 않지만, 그 값이 계산되어 나올 때 까지 Future 객체를 통해 기다려야 한다.



Callback과 Future 두 가지가 있는데 Callback은 그 결과값들을 합쳐서 사용하기가 어렵고 코드를 어렵게 만들어 유지보수하기가 쉽지 않다. (Callback 지옥 등..)




































