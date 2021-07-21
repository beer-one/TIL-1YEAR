

# BlockHound

우리가 리액티브 프로그래밍을 할 때, 의도치 않게 (또는 실력이 없어서) Blocking 코드를 삽입할 수도 있는데, Non-blocking Reactive 프로그래밍을 할 때 blocking 코드가 있다면 병목이 발생할 수도 있다. 이는 코드의 성능 저하로 이어질 수 있기 때문에 blocking 코드를 최대한 없애야 한다.

그런데 테스트 코드를 통해 blocking 코드를 감지할 수 있도록 해주는 오픈소스가 있는데 바로 Reactor에서 지원하는 [BlockHound](https://github.com/reactor/BlockHound) 이다. BlockHound는 non-blocking thread에서 blocking call을 감지하는 자바 에이전트이다.



## Dependency

```groovy
repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.projectreactor.tools:blockhound:$LATEST_RELEASE")
}
```

* 글 쓸 당시 LATEST_RELEASE = `1.0.4.RELEASE`



## Installation

BlockHound는 JVM agent이기 때문에 Blocking code가 있는지 감지하기 위해 테스트 하기 전에 agent를 install 해야 한다.

```kotlin
BlockHound.install()
```

install 후에 해당 agent가 있는 클래스의 모든 integration test code에서 blocking call을 감지한다. 보통 agent install은 `@BeforeClass` 또는 static { } 블록이나 생성자 등에서 생성하면 된다. 예시 코드는 다음과 같다.

```kotlin
class BlockHoundTest {

    private val blockHound = BlockHound.install()

    @Test
    fun `BlockHound test1`() {
        Mono.delay(Duration.ofMillis(1))
            .doOnNext {
                try {
                    Thread.sleep(10) // Blocking Code 1
                } catch (e: InterruptedException) {
                    throw RuntimeException()
                }
            }
            .block() // Blocking Code 2
    }

    @Test
    fun `BlockHound test2, with StepVerifier`() {
        val mono = Mono.delay(Duration.ofMillis(1))
            .doOnNext {
                runBlocking { // Blocking Code
                    delay(100L)
                }
            }

        StepVerifier.create(mono)
            .expectNext(0)
            .verifyComplete()
    }

    @Test
    fun `BlockHound test3, with StepVerifier, nonBlocking`() {
        val mono = Mono.just(100)
            .map { it * 2 }

        StepVerifier.create(mono)
            .expectNext(200)
            .verifyComplete()
    }
}
```

첫 번째와 두 번째 코드는 모두 Blocking code를 가지고 있다. (Thread.sleep(), block(), runBlocking { ... }) 그리고 세 번째 코드는 blocking code를 가지고 있지 않는다. 예상한대로 테스팅 결과는 세 번째 테스트코드만 통과하게 된다. 테스트 결과 로그를 보면 다음과 같이 나온다.

```
BlockHoundTest > BlockHound test1() FAILED
    reactor.core.Exceptions$ReactiveException at BlockHoundTest.kt:25
        Caused by: reactor.blockhound.BlockingOperationError at Thread.java:-1
...
reactor.blockhound.BlockingOperationError: Blocking call! java.lang.Thread.sleep
```

첫 번째 에러 로그를 보면 Blocking call을 감지하면 에러를 뱉는 것을 알 수 있다. 일단 .block()을 한다는 것 자체가 걸린다.



```
BlockHoundTest > BlockHound test2, with StepVerifier() FAILED
    java.lang.AssertionError at MessageFormatter.java:115
...
expectation "expectNext(0)" failed (expected: onNext(0); actual: onError(reactor.blockhound.BlockingOperationError: Blocking call! jdk.internal.misc.Unsafe#park))
```

두 번째 에러 로그를 보면 StepVerifier에서 걸린다. 보통 리액티브 프로그래밍을 할 때는 .block()을 하는 실수는 거의 하지 않는다. 대신 리액터 API 스트림 코드 블럭 내부에 실수로 blocking 코드를 넣는 경우가 더 많기 때문에 의도적으로 .block()을 하지 않기 위해 StepVerifier를 이용하여 리액티브 코드를 실행시킨다. 당연히 리액티브 스트림을 실행하면 doOnNext 블럭 내부의 runBlocking 코드가 있기 때문에 걸린다. 마찬가지로 actual: 뒤의 내용을 보면 Blocking call이 감지되었다는 로그가 찍힌다.





## Customization

BlockHound의 사용법은 총 3가지이다.

1. BlockHound.install() :  ServiceLoader를 사용하여 모든 `reactor.blockhound.integration.BlockHoundIntegration` 에 있는 것들을 로드한다.

2. BlockHound.install(BlockHoundIntegration ... integration): 1 과 같지만, 사용자 정의의 integration을 추가로 로드할 수 있다. (직접 구현체를 작성해서 넣는 것 가능)
3. BlockHound.builder().install() : 어떠한 integration을 로드하지 않고 builder를 만들어서 생성. 보통 `BlockHound.builder().with(...).with(...).install()` 과 같은 방식을 사용.



### 특정 메서드를 blocking 메서드 취급하기

builder 패턴으로 BlockHound를 install 할 때, 어떤 특정 메서드를 blocking 메서드로 취급하도록 설정할 수 있다.

- `Builder#markAsBlocking(Class clazz, String methodName, String signature)`
- `Builder#markAsBlocking(String className, String methodName, String signature)`



### 어떤 메서드 안에있는 blocking 코드 허용하기(하지 않기)

어떤 메서드 안에서는 blocking 코드를 허용하게(허용하지 않게) 할 수 있다.

- `Builder#allowBlockingCallsInside(String className, String methodName)`
- `Builder#disallowBlockingCallsInside(String className, String methodName)`

