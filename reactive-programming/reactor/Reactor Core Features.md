# Reactor Core Features

Reactor는 Java 8 이상에서 사용이 가능하다. Reactor가 Java8 함수형 API(CompletableFuture, Stream, Duration)와 관련이 있기 때문일 것이다. 



Reactor 라이브러리를 사용하기 위해서는 아래와 같이 gradle 설정을 해준다. (KotlinDSL)

```groovy
plugins {
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
}

dependencies {
    implementation("io.projectreactor:reactor-core:3.3.11.RELEASE")
}
```



Reactor에서는 Publisher를 구현함과 동시에 풍부한 연산자도 제공해주는 조합 가능한 reactive type을 제공하는데 이것이 바로 `Mono` 와 `Flux` 이다. **Mono** 는 0 또는 1([0,1])개의 항목을 가지는 리액티브 시퀀스를 나타내며, **Flux** 는 여러 개([0,N]) 항목을 가지는 리액티브 시퀀스를 나타낸다.



## Mono와 Flux

### Flux

**Flux\<T>** 는 [0,N] 개의 항목에 대한 비동기 시퀀스를 나타내는 **Publisher\<T>** 의 표준이며, 선택적으로 완료 시그널이나 에러를 보내서 종료시킬 수 있다. Reactive Stream 스펙에서 Publisher는 데이터를 전달하는 `onNext`, 데이터가 완료되었다는 것을 전달하는  `onComplete`, 에러를 전달하는  `onError` 를 트리거할 수 있는데, 이는 각각 Subscriber의 `onNext()`, `onComplete()`, `onError()` 메서드로 전달된다.

Flux는 가능한 신호의 범위가 넓기 때문에 Flux는 범용 Reactive type이다. **onNext** 없이 **onComplete** 만 트리거되도록 하면 빈 시퀀스를 표현할 수 있으며, **onComplete** 만 없다면 무한한 시퀀스를 표현할 수 있다.

![image-20210831233133463](/Users/yunseowon/Library/Application Support/typora-user-images/image-20210831233133463.png)



### Mono

**Mono\<T>** 는 `onNext` 시그널을 통해 **최대 하나**의 항목만 방출한 후 `onComplete` 시그널과 함께 종료되거나 `onError` 시그널로 에러를 방출시키는 **Publisher\<T>** 이다. 

대부분의 Mono 구현은 Subscriber에서 onNext가 호출된 후  즉시 onComplete가 호출될 것이다. 번외로, **Mono.never()** 가 있는데, 이는 어떠한 시그널도 방출하지 않는다.

Mono는 오직 Flux가 사용할 수 있는 operator 중 일부만 제공한다. 그리고 일부 operator는 Flux로 형변환하는 것들도 있다. Mono로 단순 실행만 하는 값이 없는 비동기 프로세스를 표현할 수 있는데, 이를 표현하려면 **Mono\<Void>** 를 사용하면 된다.

![image-20210831234249037](/Users/yunseowon/Library/Application Support/typora-user-images/image-20210831234249037.png)





### Flux (Mono) 생성

Flux와 Mono를 만드는 가장 쉬운 방법은 Flux(Mono)에서 제공하는 Factory 메서드를 사용하는 방법이다. 

Flux는 `just(...)` 메서드로 가변인자를 사용하여 생성하거나 `fromIterable(iterable)` 메서드를 통해 Flux를 생성할 수 있다. 또는 `range()` 메서드를 통해 범위를 가지는 Flux를 생성할 수 있다.

```kotlin
val alcohols = Flux.just("beer", "soju", "wine")

val sportList = listOf("Soccer", "Baseball", "Basketball")
val sportFlux = Flux.fromIterable(sportList)

val rangeFlux = Flux.range(5, 10) // 5 ~ 14 
```





### subscribe

Flux(Mono) 객체를 구독하는 subscribe 메서드는 여러가지가 있는데 하나하나 살펴보자.

```java
subscribe(); // (1): 시퀀스를 구독하고 트리거한다.

subscribe(Consumer<? super T> consumer); // (2): 방출된 값 각각으로 어떤 행동을 한다.

subscribe(Consumer<? super T> consumer,
          Consumer<? super Throwable> errorConsumer); // (3): (2) + 에러가 발생할 때는 별도의 행동을 한다.

subscribe(Consumer<? super T> consumer,
          Consumer<? super Throwable> errorConsumer,
          Runnable completeConsumer); // (4): (3) + 시퀀스가 완료되었을 때는 또 다른 행동을 한다.

subscribe(Consumer<? super T> consumer,
          Consumer<? super Throwable> errorConsumer,
          Runnable completeConsumer,
          Consumer<? super Subscription> subscriptionConsumer);
// (5): (4) +  몇 개의 데이터를 구독할 것인지 (취소할 것인지) 정의
```



먼저 가장 위에있는 subscribe()는 Publisher를 구독하고 트리거해서 값을 받아온다. 

```kotlin
val intFlux = Flux.range(1, 5).log()

intFlux.subscribe()
```

```
[ INFO] (main) | onSubscribe([Synchronous Fuseable] FluxRange.RangeSubscription)
[ INFO] (main) | request(unbounded)
[ INFO] (main) | onNext(1)
[ INFO] (main) | onNext(2)
[ INFO] (main) | onNext(3)
[ INFO] (main) | onNext(4)
[ INFO] (main) | onNext(5)
[ INFO] (main) | onComplete()
```

Flux.log() 메서드는 방출되는 item 각각을 로깅하는 메서드이다. 공부할 때 찍어두면 좋다. 

subscribe()를 하는 순간 flux가 방출하는 item을 하나하나 받아온다. log() 메서드를 없애면 콘솔에 아무것도 찍히지 않기 때문에 log() 메서드를 사용하였다.



두 번째 subscribe()는 Flux에서 방출한 값 각각에 대해 어떤 행동을 정의할 수 있다. 여기서는 방출된 값 하나하나를 Print 한다.

```kotlin
val intFlux = Flux.range(1, 5)

intFlux.subscribe { num ->
    println("item: $num")
}
```

```
item: 1
item: 2
item: 3
item: 4
item: 5
```



세 번째 subscribe() 는 Flux에서 에러 신호를 보낼 경우 별도로 어떤 행동을 정의할 수 있다. Flux에서 에러를 방출하는 순간 구독은 종료된다.

```kotlin
class NumberException: RuntimeException("숫자가 왜 4지?")

val intFlux = Flux.range(1, 5)
.handle<Int> { num, sink ->
    if (num == 4) sink.error(NumberException())
    else sink.next(num)
}

intFlux.subscribe(
    { num ->
        println("Item: $num")
    },
    { error ->
        println("Error message: ${error.message}")
    }
)
```

```
Item: 1
Item: 2
Item: 3
Error message: 숫자가 왜 4지?
```



네 번째 subscribe()에서는 완료 신호에 대한 행동도 정의할 수 있다.

```kotlin
val intFlux = Flux.range(1, 5)
    .handle<Int> { num, sink ->
        if (num == 10) sink.error(NumberException())
        else sink.next(num)
    }

intFlux.subscribe(
    { num ->
        println("Item: $num")
    },
    { error ->
        println("Error message: ${error.message}")
    },
    {
        println("subscription is completed")
    }
)
```

```
Item: 1
Item: 2
Item: 3
Item: 4
Item: 5
subscription is completed
```



다섯 번째 subscribe() 에서는 몇 개의 데이터를 구독할 것인지 또는 구독 취소를 할 것인지 정할 수 있다. 

```kotlin
val intFlux = Flux.range(10, 50).log()
    .handle<Int> { num, sink ->
        if (num == 1000) sink.error(NumberException())
        else sink.next(num)
    }

intFlux.subscribe(
    { num ->
        println("Item: $num")
    },
    { error ->
        println("Error message: ${error.message}")
    },
    {
        println("subscription is completed")
    },
    { sub ->
        sub.request(5)
        // sub.cancel()
    }
)
```

```
[ INFO] (main) | onSubscribe([Synchronous Fuseable] FluxRange.RangeSubscriptionConditional)
[ INFO] (main) | request(5)
[ INFO] (main) | onNext(10)
Item: 10
[ INFO] (main) | onNext(11)
Item: 11
[ INFO] (main) | onNext(12)
Item: 12
[ INFO] (main) | onNext(13)
Item: 13
[ INFO] (main) | onNext(14)
Item: 14
```

로그를 보면 `sub.request(5)` 로 설정을 하면 onSubscribe 다음에 request(5)가 찍혀있다. 위의 4개 예시에서 로그를 추가로 찍어보면 request(unbounded)로 찍혀있을 건데 default = unbounded이다. unbounded는 Publisher가 `onComplete` 또는 `onError` 시그널을 보낼 때 까지 구독하는 것이고 unbounded가 아닌 정수값인 경우 설정한 개수만큼 데이터를 받아올 때 까지 구독한다는 의미이다.

물론 지정한 데이터 개수만큼 받아오기 전에 에러가 나거나 완료 시그널을 받으면 구독이 종료된다.



### subscribe 취소

위에서 언급된 subscribe() 메서드는 `Disposable` 인터페이스를 리턴한다. Disposable 인터페이스는 `dispose()` 메서드를 호출하여 구독을 취소할 수 있다. Flux나 Mono 객체에서의 취소는 데이터를 생산해내는 Publisher가 데이터를 생산하는 것을 멈추게 하는 신호이다. 하지만 취소를 한다고 해서 곧바로 Publisher가 데이터를 생산하는 것을 멈추진 않는다. 그래서 어떤 경우에는 Publisher가 취소 신호를 받기 전에 데이터를 이미 모두 방출해서 완료가 되는 경우도 있다.

`Disposables` 클래스는 Disposable 인터페이스와 관련된 여러 기능들이 있는데, `Disposables.swap()` 메서드는 Disposable 구현체를 원자적으로 취소하고 대체할 수 있는`Disposable` 래퍼 객체를 생성한다. 그리고 `Disposables.composite(...)` 메서드는 여러 개의 `Disposable` 인터페이스를 모을 수 있도록 해준다. Disposables.composite() 를 호출하면 `Disposable.Composite` 인터페이스가 리턴되는데 `Disposable.Composite.dispose()` 를 호출하면 composite 된 모든 Disposable이 취소된다.



### BaseSubscriber

람다식으로 subscriber를 구성하는 방법 외에 더 일반적인 기능을 사용할 수 있도록 Subscriber를 구성할 수도 있다. Reactor에서는 이러한 Subscriber를 구성하기 위해서 `BaseSubscriber` 클래스를 상속하는 클래스를 구현하면 된다. 

그런데 주의할 점은 BaseSubscriber 인스턴스는 다른 Publisher를 구독한 경우에는 이미 구독 중인 Publisher의 구독을 취소하기 때문에 재활용이 불가능하다. 인스턴스에 여러 Publisher를 구독한다면 구독자의 onNext 메서드가 병렬로 호출되어야 하는데 이는 onNext 메서드가 병렬로 호출되지 않아야 한다는 Reactive Stream의 규칙을 위반하기 때문이다. 

먼저 BaseSubscriber를 상속하는 클래스를 간단히 구현해보자.

```kotlin
class SampleSubscriber<T>(
    private val exitValue: T
): BaseSubscriber<T>() {

    override fun hookOnSubscribe(subscription: Subscription) {
        println("Subscribed")
        request(1)
    }

    override fun hookOnNext(value: T) {
        println(value)

        if (value != exitValue)
            request(1)
    }
   
    override fun hookOnError(throwable: Throwable) { }

    override fun hookOnCancel() { }

    override fun hookOnComplete() { }

    override fun hookFinally(type: SignalType) { }
}
```

보통 BaseSubscriber 를 커스텀 할 때 최소한 **hookOnSubscribe()** 메서드와 **hookOnNext** 메서드는 오버라이딩 하여 구현해줘야 한다.

**hookOnSubscribe()** 메서드는 처음 구독 후 몇개의 데이터를 요청할 것인지 request 이벤트를 보내는 메서드이다. **HookOnNext()** 메서드는 onNext 이벤트를 받아 데이터를 처리하는 기능을 한다. 여기서는 데이터 처리 로직을 구현하면 된다. 추가로, request 메서드로 몇개의 데이터를 더 요청하기 위해 request 메서드를 사용할 수도 있다. 예시에서는 exitValue를 받으면 더 이상 request를 하지 않도록 구현되어 있다.



실제로 SampleSubscribe로 구독하는 코드는 아래에 있다. 4의 값을 받으면 출력 후 더 이상 request를 받지 않는다.

```kotlin
val sampleSubscriber = SampleSubscriber<Int>(4)
val intFlux = Flux.range(1, 10)

intFlux.subscribe(sampleSubscriber)
```

``` 
Subscribed
1
2
3
4
```



## Backpressure

Reactor에서 Backpressure를 구현할 때, Consumer의 요청이 source로 다시 전파되는 방법은 upstream operator에게 `request`를 보내는 것이다. 현재 요청의 합은 현재의 `demand` 또는 `pending request` 로 불리기도 한다. `Demand`는 제한 없는 요청을 나타내는 `Long.MAX_VALUE`로  제한된다.



최초 요청이 구독 시점에서 마지막 subscriber으로 전달되지만 모두 구독하는 가장 직접적인 방법은 즉시 제한없는 요청을 트리거하는 것이다. 트리거 하는 방법은 아래와 같이 여러가지 방식이 있다.

* subscribe() 메서드 (람다식)
* block(), blockFirst(), blockLast()
* toIterable(), toStream()과 같은 iterating



가장 간단한 방법은 위에서 했던 것과 같이 `BaseSubscriber` 를 상속하는 Subscriber를 직접 구현해서 사용하는 방식이다. 



### Downstream에서 요청 변경

Reactor에서는 데이터를 구독하는 단계에서 표현된 수요를 각 upstream chain에서의 operator를 통해 변형할 수 있다. 대표적인 예시로는 `buffer(N)` 이다.



#### buffer(N)

`buffer(N)` 은 upstream애서 방출된 데이터가 N개 모이면 downstream으로 방출하게 하는 operator이다. 만약, upstream에서 N개의 데이터를 방출하지 못하고 complete 된다면 방출한 만큼의 데이터를 downstream으로 보내게 된다.  `buffer(N)` 가 upstream에서 받은 데이터를 버퍼 단위로 방출하기 때문에 리턴 타입은 `Mono<List<T>>` 또는 `Flux<List<T>>` 이다.

```kotlin
fun main() {
    val LIMIT = 37
    var current = 0
    val upstream = Flux.generate<Int> {
        runBlocking { delay(100L) }
        if (current == LIMIT) it.complete()
        current++
        it.next(Random.nextInt(1, 10000))
    }

    upstream.buffer(10) // downStream
        .subscribe(TestSubscriber<List<Int>>())
}

class TestSubscriber<T>: BaseSubscriber<T>() {

    override fun hookOnSubscribe(subscription: Subscription) {
        println("Subscribed")
        request(2)
    }

    override fun hookOnNext(value: T) {
        println("onNext")
        println(value)
        request(2)
    }
}
```

* upstream은 Int형인데 buffer(N)을 구독한 subscriber의 제네릭 타입은 List\<Int> 이다.
* buffer(N)은 upstream에게 request(2N) 신호를 보낸다.
* LIMIT 변수를 보면 upstream은 37개의 데이터를 방출시키고 completed된다.
* subscriber에서 출력된 결과를 보면 버퍼단위로 받고 있으며, 마지막 7개는 upstream에서 10개의 데이터를 방출하지 못하고 complete가 되었기 때문에 completed 된 시점에서 7개의 데이터가 담긴 버퍼를 방출하게 된다.

```
Subscribed
onNext
[9471, 682, 3483, 9702, 3783, 9479, 2833, 5835, 7738, 8469]
onNext
[9051, 9560, 6650, 1558, 5649, 6366, 6968, 728, 1999, 9710]
onNext
[6734, 1440, 5919, 2417, 5356, 1053, 6644, 2899, 4163, 878]
onNext
[5822, 7684, 2519, 9596, 2994, 5614, 2678]
```







만약 Publisher에서 `request(2)` 를 요청받았다면, 

 만약 `request(2)`를 받았다면 두 개의 전체 버퍼에 대한 요청으로 해석된다. 버퍼가 꽉 차기 위해서 N개의 요소가 필요하므로, buffer 연산자는 요청을 2N개로 변형한다.



#### prefetch

그리고 downstream에서는 upstream에게 prefetch 매개변수를 이용하여 upstream에게 request를 보내 미리 upstream이 데이터를 방출하도록 시킬 수 있다. prefetch는 `Flux.publishOn(scheduler, prefetch)` 메서드를 아용하면 설정할 수 있다. publishOn에서 설정하지 않는다면 기본적으로 MathMax(16, 256)으로 설정될 것이다. (코드 까보면 그렇게 나와있음)

prefetch는 Publisher에서 요청받을 데이터를 선 반영하는 전략이며, 이는 `Replenishing Optimization` 을 구현한다. operator가 prefetch의 75%정도를 수행했다면, prefetch의 75%정도를 다시 upstream에게 미리 요청하는 전략이다. 이게 무슨의미인지 처음엔 잘 모를 수도 있는데 간단히 예시를 보자.

먼저 아래 코드는 prefetch를 하지 않는 publisher-subscriber 에 대한 예시인데, upstream에서 **데이터를 방출하는 데 걸리는 시간은 1000ms** 라고 가정하자. 그리고 subscriber에서는 **데이터를 처리하고 request를 하는 시간까지 걸리는 시간을 500ms**라고 하면 **request 사이의 간격은 총 1500ms** 정도가 될 것이다.

```kotlin
fun main() {
    var i = 1
    val upstream = Flux.generate<Int> {
        runBlocking { delay(1000L) }
        it.next(i++)
    }

    val downstream = upstream
        //.publishOn(Schedulers.boundedElastic(), 4)

    downstream.subscribe(TestSubscriber<Int>())
}

class TestSubscriber<T>: BaseSubscriber<T>() {
    var time = System.currentTimeMillis()

    override fun hookOnSubscribe(subscription: Subscription) {
        println("Subscribed")
        time = System.currentTimeMillis()
        request(1)
    }

    override fun hookOnNext(value: T) {
        println("onNext")
        val fetchTime = System.currentTimeMillis() - time
        println("value: $value, fetchTime: $fetchTime")
        time = System.currentTimeMillis()

        runBlocking { delay(500L) }
        request(1)
    }
}
```

```
[main] INFO reactor.Flux.Generate.1 - | request(1)
Subscribed
[main] INFO reactor.Flux.Generate.1 - | onNext(1)
onNext
value: 1, fetchTime: 1053
[main] INFO reactor.Flux.Generate.1 - | request(1)
[main] INFO reactor.Flux.Generate.1 - | onNext(2)
onNext
value: 2, fetchTime: 1506
[main] INFO reactor.Flux.Generate.1 - | request(1)
[main] INFO reactor.Flux.Generate.1 - | onNext(3)
onNext
value: 3, fetchTime: 1508
[main] INFO reactor.Flux.Generate.1 - | request(1)
[main] INFO reactor.Flux.Generate.1 - | onNext(4)
...
```



하지만 prefetch를 적용시키면 subscriber가 데이터를 처리하는 동안에도 미리 publisher에게 request 신호를 전달하기 때문에 더 빨리 데이터를 받을 수 있다.

```kotlin
fun main() {
    var i = 1
    val upstream = Flux.generate<Int> {
        runBlocking { delay(1000L) }
        it.next(i++)
    }.log()

    val downstream = upstream
        .publishOn(Schedulers.boundedElastic(), 4)

    downstream.subscribe(TestSubscriber<Int>())
}

class TestSubscriber<T>: BaseSubscriber<T>() {
    var time = System.currentTimeMillis()

    override fun hookOnSubscribe(subscription: Subscription) {
        println("Subscribed")
        time = System.currentTimeMillis()
        request(1)
    }

    override fun hookOnNext(value: T) {
        println("onNext")
        val fetchTime = System.currentTimeMillis() - time
        println("value: $value, fetchTime: $fetchTime")
        time = System.currentTimeMillis()

        runBlocking { delay(500L) }
        request(1)
    }
}
```

```kotlin
[main] INFO reactor.Flux.Generate.1 - | onSubscribe([Fuseable] FluxGenerate.GenerateSubscription)
[main] INFO reactor.Flux.Generate.1 - | request(4) // 100% prefetch
Subscribed
[main] INFO reactor.Flux.Generate.1 - | onNext(1)
onNext
value: 1, fetchTime: 1056
[main] INFO reactor.Flux.Generate.1 - | onNext(2)
onNext
value: 2, fetchTime: 1002
[main] INFO reactor.Flux.Generate.1 - | onNext(3) // 75% prefetch 수행
onNext
value: 3, fetchTime: 1005
[boundedElastic-1] INFO reactor.Flux.Generate.1 - | request(3) // 75% Re-prefetch
[main] INFO reactor.Flux.Generate.1 - | onNext(4)
onNext
value: 4, fetchTime: 1005
[main] INFO reactor.Flux.Generate.1 - | onNext(5)
onNext
value: 5, fetchTime: 1002
[main] INFO reactor.Flux.Generate.1 - | onNext(6)
```

prefetch(4)를 적용하면 최초로 request(4) 신호를 보낸다. 그리고 subscriber는 위와는 다르게 prefetch를 했기 때문에 request()를 보내지 않고 onNext()만 수행하여 미리 fetch된 데이터를 받을 수 있다.



#### limitRate

`limitRate(N)` 은 downstream request들을 쪼개어 작은 배치단위로 upstream에 전파되도록 한다. 예를 들어, `limitRate(10)` 에 대해 100개의 요청을 하면 최대 10개의 요청이 10번 upsteam으로 전파된다. limitRate는 실제로 앞에서 다룬 replenishing optimization을 구현한다. 

반면에 `limitRequest(N)` 은 downstream 요청을 최대 총 수요로 제한한다. 이는 N개 까지의 요청을 합한다. 하나의 요청으로 전체 수요가 N개를 초과하지 않는 경우, 특정 요청이 전체적으로 upstream에게 전파된다. 그 양이 source에서 방출된 후, limitRequest는 시퀀스가 완료된 것으로 간주하고 onComplete 신호를 downstream으로 보낸 후 source를 취소한다.

