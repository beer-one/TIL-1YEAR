# Reactor

https://projectreactor.io/docs/core/release/reference/

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





## Reactive Programming 으로의 변화

Reactor와 같은 Reactive 라이브러리들은 JVM의 비동기 접근방식의 단점을 해결하는 동시에 몇 가지 측면에 초점을 맞추는걸 목표로 하여 개발되었다.



### Composability and Readability(가독성과 Task 구성능력?)

Composability는 이전 task의 결과를 사용하여 그 결과를 다음 task에 사용할 수 있도록 하는 여러가지 비동기 task들을 조율하는 능력을 의미한다. 그리고 이 몇가지 task들을 fork-join 하는 형식으로 실행시킬 수 있다. 그리고 비동기 task들을 개별적인 컴포넌트로써 재사용할 수 있다.

여러 task들을 조율하는 능력은 코드의 가독성과 유지능력과 관련되어있다. 비동기 프로세스의 개수와 복잡성이 모두 증가하면 코드를 읽기 어려워지고, 유지하는데 어려움이 발생한다. (콜백함수가 많아지면 콜백 지옥이 만들어진다거나..) Reactor는 코드로 추상 프로세스를 구성할 수 있고, 모든 태스크들이 같은 레벨로 유지될 수 있도록 풍부한 구성 옵션들을 제공한다. (Mono와 Flux의 여러 연산들..?)



### The Assembly Line Analogy[^1]

Reactive 애플리케이션에 의해 처리되는 데이터를 조립 라인을 통과하는 것으로 생각할 수 있다. 공식문서에서 Reactor는 컨베이어 벨트와 워크스테이션으로 비유하였는데, 최초의 데이터는 source(Publisher)로 들어가서 처리가 완료된 데이터는 consumer(Subscriber)로 푸쉬될 준비를 한다. 

최초의 데이터는 여러가지 변형과정을 거치거나 여러 조각으로 분해되거나 여러 조각을 함께 모으는 작업을 거칠 수도 있다. 하나의 지점에 결함이나 문제가 발생하는 경우에 문제가 발생한 워크스테이션에서 데이터의 흐름을 제한하기 위해 upstream에 신호를 보낼 수도 있다.



### Operators

Reactor에서 Operator는 조립라인의 워크스테이션이다. 각 Operator는 Publisher에게 데이터 처리 과정을 더하고 이전 스텝의 Publisher를 새로운 인스턴스로 감싼다. 그래서 전체의 체인들은 연결되어있다. 데이터는 처음 Publisher를 통과하여 여러 체인을 거치면서 데이터가 변형된다. 결국, Subscriber는 모든 체인을 통과하고 나온 결과를 받는다. 그런데 여기서 주의해야 할 점은 subscriber가 Publisher를 구독하지 않으면 아무 일도 발생하지 않는다. 

Reactive Stream 사양은 operator를 지정하지 않지만, Reactor와 같은 reactive 라이브러리의 장점 중 하나는 operator가 제공하는 풍부한 표현 방식이다. 이런 표현방식은 단순 변환과 필터링을 포함하여 여러 복잡한 orchestration과 오류 처리 등 여러가지를 표현할 수 있다.



### Nothing Happens Until You subscribe()

Reactor에서 Publisher 체인을 작성하면 그냥 단순히 비동기 프로세스를 만들어 놓을 뿐이지 데이터는 기본적으로 Publisher를 통과하여 처리되지 않는다. Subscriber가 subscribe를 하면 Publisher를 Subscriber에 연결하여 전체 chain에서 데이터 흐름을 유발한다. 이는 upstream으로 전파되는 subscriber의 단일 요청 신호에 의해 내부적으로 처리되고, 다시 Publisher에게 전달된다.



### Backpressure

upstream에 신호를 전파하는 것은 backpressure를 개발하여 사용할 수 있는데, backpressure는 조립라인에서 워크스테이션이 upstream 워크스테이션보다 더 느리게 처리될 때 전송되는 피드백 신호와 비슷하다.

Subscriber가 무한히 많은 데이터를 구독할 수 있으며, Publisher가 아주 빠른 속도로 푸쉬하도록 하거나 요청 메커니즘을 사용하여 약 n개의 요소를 처리할 준비가 되어있다는 신호를 보낼 수 있는데, Reactive Stream 스펙에서 정의되는 실제 메커니즘은 이와 유사하다. 

중간 operator는 전송 중인 요청을 변경할 수도 있다. 예를 들어, 데이터를 10개 묶음으로 그룹화하는 buffer operator를 생각해보자. subscriber가 하나의 버퍼를 요청한다면, source가 10개의 데이터를 생산하는 것이 허용된다. 일부 oprator는 `request(1)` round-trip을 방지하고 요청되기 전에 요소를 생산하는 것이 비용이 많이 들지 않으면 prefetching 전략을 구현한다.

이는 push 모델을 push-pull 하이브리드 모델로 변형하는데, 요소가 이미 사용 가능하다면 downstream은 n개의 요소를 upstream으로부터 받아올 수 있다. 하지만, 요소가 당장 사용가능하지 않다면 생산될 때 마다 upstream에서 요소들을 푸쉬 해준다.



### Hot vs Cold

Reactive 라이브러리는 `Hot` 과 `Cold` 라는 두 가지의 리액티브 시퀀스를 구별한다. 이러한 구분은 주로 reactive stream이 subscriber에게 어떻게 반응하는지와 관련된다.

* **Cold** sequence는 각 subscriber에게 데이터 소스를 포함하여 시퀀스가 새로 시작된다. 예를 들어, source가 HTTP 통신을 사용한다면, 새로운 HTTP 요청이 각 구독마다 새로 만들어진다.
* **Hot** sequence는 각 subscriber에 대해 시퀀스가 처음부터 시작되지 않는다. 대신에, 나중에 들어온 subscriber는 구독 후 방출되는 신호를 수신한다. 그러나, Hot reactive stream은 전체 또는 일부 방출된 이력을 캐싱하거나 재현할 수 있다. Hot sequence는 아무도 구독하지 않은 경우에도 Hot sequence가 방출될 수 있다.



































---

[^1]: 비유, 유사점

