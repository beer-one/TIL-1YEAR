# Reactor Core Features 2

이번 장에서는 Mono와 Flux를 직접 프로그래밍으로 만들어보는 것을 소개하겠다. 



## Synchronous generate

Flux를 생성하는 가장 간단한 방법은 `generate` 메서드를 이용하는 것이다. generate 메서드는 동기적인 방식이며, 하나씩 방출하는 방식이다. 즉, sink는 `SynchronousSink` 이고 `sink.next()` 메서드는 콜백 호출 당 최대 한 번만 호출될 수 있다. 그리고 stream 종료를 알리기 위해 선택적으로 `sink.error()` 또는 `sink.complete()` 메서드를 호출할 수도 있다.

generate() 메서드를 사용하면서 가장 유용한 점은 다음에 무엇을 방출할지 결정하기 위해 sink를 사용할 때 참조할 수 있는 상태를 유지할 수 있도록 상태변수를 사용할 수 있다는 점이다. 

`Flux.generate()` 메서드는 초기 상태를 제공하는 메서드인 `Supplier<S>` 와 다음 sink를 생성하는 generator 함수로 구성되어있는데, generator 함수는 `BiFunction<S, SynchronousSink<T>>` 로 구성되어있으며, `<S>` 는 상태변수의 타입이다. generator 함수의 리턴값은 다음 단계의 상태가 된다.



아래는 소수를 방출하는 Flux를 구현하였다.

```kotlin
val primeFlux = Flux.generate<Int, MutableSet<Int>>(
    { mutableSetOf() },
    { primeSet, sink ->
        var next = primeSet.maxOrNull()?.plus(1)?: 0

        if (next == 0) {
            sink.next(2)
            primeSet.add(2)
        } else {
            while (primeSet.any { next % it == 0 }) {
                next++
            }
            primeSet.add(next)
            sink.next(next);
        }
        primeSet
    }
)
```



## Asynchronous and Multi-threaded create

`create` 메서드는 멀티쓰레드에서 round당 여러개를 배출하는 것에 적합한 Flux 생성 함수이다. 이는 next, error, complete 메서드와 함께 FluxSink를 노출한다. `generate` 와는 다르게, `create` 는 상태기반 생성 함수가 아니고 callback에서 멀티쓰레드 이벤트를 트리거할 수 있다.

주의해야 할 점은 `create` 메서드가 비동기 API에서 사용할 수 있지만 `create` 메서드는 코드를 병렬화 하지 않으며 비동기화 하지도 않는다. create 람다 내에서 block을 한다면, 데드락과 같은 side-effect에 빠질 수 있다. subscribeOn 을 사용하더라도 long-blocking create lambda가 파이프라인을 lock 할 수 있으므로 주의해야 한다. 















### Handle

`handle` 메서드는 Mono(Flux)의 인스턴스 메서드로서, 기존 스트림에 어떤 로직을 추가하여 핸들링할 때 사용한다. 이 메서드는 `generate` 메서드와 유사하며, `SynchronousSink`를 사용하고 오직 1:1 방출만 허용한다. 대신, `handle` 메서드는 각 source의 요소에서 임의의 값을 생성할 수 있고, 일부 요소는 건너뛸 수도 있다. 그래서 handle 서드는 map과 filter 역할을 모두 하는 만능 메서드이다.

```java
Flux<R> handle(BiConsumer<T, SynchronousSink<R>>);	
```



간단히 1부터 100까지 숫자 중 5의배수를 제외한 숫자를 필터링하는 Flux를 handle로 구현해보았다. 건너뛸 요소는 sink.next() 를 하지 않으면 된다.

```kotlin
Flux.fromIterable(1..100)
    .handle<Int> { num, sink ->
        if (num % 5 != 0) sink.next(num)
    }
    .subscribe { println(it) }
```





### Threading and Schedulers

Reactor는 동시성 모델을 지향하지(?) 않지만 라이브러리가 동시성을 막지는 못한다. 

Flux(Mono)를 얻는다고 해서 반드시 전용 쓰레드에서 실행되는 것은 아니다. 대신에, 대부분의 연산자는 이전 연산자가 실행한 쓰레드에서 계속 작업한다. 쓰레드를 지정하지 않는다면, 가장 최상위 연산자 자신은 subscribe() 를 호출한 쓰레드에서 실행될 것이다. 아래 예시는 Mono를 새로운 쓰레드에서 실행하는 예시이다.

```kotlin
fun main() {
    val mono = Mono.just("Hello")

    val thread = Thread {
        mono.map { "$it thread" }
            .subscribe { println("$it ${Thread.currentThread().name}") }
    }

    thread.start()
    thread.join()
}
```

```
Hello thread Thread-0
```



Reactor에서는 실행 모델과 실행하는 장소 (쓰레드?)는 Scheduler가 결정한다. Scheduler는 ExecuterService와 유사하게 스케쥴링하는 책임을 가지고 있지만 전용 추상화를 사용하면 더 많은 역할을 수행한다. 

`Schedulers` 클래스는 여러가지 스케쥴러를 static method로 제공한다.

* `Schedulers.immediate()`: 처리 시간에 현재 쓰레드에서 곧바로 실행된다. subscribeOn()을 하지 않으면 default로 Schedulers.immediate()를 사용한다.
* `Schedulers.single()`: 스케쥴러가 삭제될 때 까지 동일한 쓰레드를 재사용한다. 호출 당 쓰레드를 새로운 것을 사용하려면 `Schedulers.newSingle()`을 사용하면 된다.

* `Schedulers.elastic()`: Unbounded elastic thread pool을 사용한다. 이 스케쥴러는 backpressure에 관한 문제를 많이 숨기고 너무 많은 쓰레드가 사용될 수 있기 때문에 boundedElastic을 지원하는 이후에는 선호되지 않는 스케쥴러이다. 
* `Schedulers.boundedElastic()` : bounded elastic thread pool을 사용한다. elastic()과 마찬가지로 필요에 따라 새로운 worker pool을 만들고 idle 상태의 pool을 재사용한다. 많은 시간동안 idle 상태에 있는 worker pool은 삭제된다. (defalut: 60s) 그리고 elastic()과 다른 점은 쓰레드 개수가 제한되는데, 이는 default로 CPU 코어 * 10개 까지 제한한다. 쓰레드가 전부 사용된다면 최대 10만개의 작업이 대기되며, 쓰레드가 사용 가능해지면 다시 스케쥴링 할 수 있다. 이 스케쥴러는 I/O 블로킹 작업을 사용할 때 좋은 선택이 될 수 있다. boundedElastic()은 blocking 프로세스가 다른 리소스와 연결되지 않도록 자체 쓰레드를 제공하는 편리한 방식이다. 
* `Schedulers.parallel()` : 동시성 작업에 특화된 고정 worker pool이다. 이는 CPU 코어 개수만큼의 worker가 생성된다.

* `Schedulers.fromExecuterService()` : 특정 ExecuterService로 스케쥴링한다.

```kotlin
val logger = LoggerFactory.getLogger("Logger")

val flux = Flux.fromIterable(1..5)
    .handle<Int> { num, sink ->
        runBlocking { delay(100L) }
        sink.next(num)
    }

flux.subscribeOn(Schedulers.immediate())
    .subscribe { logger.info("1: $it") }
flux.subscribeOn(Schedulers.immediate())
    .subscribe { logger.info("2: $it") }
```

```
[main] INFO Logger - 1: 1
[main] INFO Logger - 1: 2
[main] INFO Logger - 1: 3
[main] INFO Logger - 1: 4
[main] INFO Logger - 1: 5
[main] INFO Logger - 2: 1
[main] INFO Logger - 2: 2
[main] INFO Logger - 2: 3
[main] INFO Logger - 2: 4
[main] INFO Logger - 2: 5
```

```kotlin
val logger = LoggerFactory.getLogger("Logger")
logger.info("Start")

val flux = Flux.fromIterable(1..5)
    .handle<Int> { num, sink ->
        runBlocking { delay(100L) }
        sink.next(num)
    }

flux.subscribeOn(Schedulers.single())
    .subscribe { logger.info("1: $it") }
flux.subscribeOn(Schedulers.single())
    .subscribe { logger.info("2: $it") }

runBlocking { delay(10000) }
```

```
[main] INFO Logger - Start
[single-1] INFO Logger - 1: 1
[single-1] INFO Logger - 1: 2
[single-1] INFO Logger - 1: 3
[single-1] INFO Logger - 1: 4
[single-1] INFO Logger - 1: 5
[single-1] INFO Logger - 2: 1
[single-1] INFO Logger - 2: 2
[single-1] INFO Logger - 2: 3
[single-1] INFO Logger - 2: 4
[single-1] INFO Logger - 2: 5
```

```kotlin
val logger = LoggerFactory.getLogger("Logger")
logger.info("Start")

val flux = Flux.fromIterable(1..5)
    .handle<Int> { num, sink ->
        runBlocking { delay(100L) }
        sink.next(num)
    }


flux.subscribeOn(Schedulers.newSingle("singleThread"))
    .subscribe { logger.info("1: $it") }
flux.subscribeOn(Schedulers.newSingle("singleThread"))
    .subscribe { logger.info("2: $it") }

runBlocking { delay(10000) }
```

```
[main] INFO Logger - Start
[singleThread-1] INFO Logger - 1: 1
[singleThread-2] INFO Logger - 2: 1
[singleThread-2] INFO Logger - 2: 2
[singleThread-1] INFO Logger - 1: 2
[singleThread-2] INFO Logger - 2: 3
[singleThread-1] INFO Logger - 1: 3
[singleThread-1] INFO Logger - 1: 4
[singleThread-2] INFO Logger - 2: 4
[singleThread-1] INFO Logger - 1: 5
[singleThread-2] INFO Logger - 2: 5
```

```kotlin
val logger = LoggerFactory.getLogger("Logger")
logger.info("Start")

val flux = Flux.fromIterable(1..5)
    .handle<Int> { num, sink ->
        runBlocking { delay(100L) }
        sink.next(num)
    }


flux.subscribeOn(Schedulers.elastic())
    .subscribe { logger.info("1: $it") }
flux.subscribeOn(Schedulers.elastic())
    .subscribe { logger.info("2: $it") }

runBlocking { delay(10000) }
```

```
[main] INFO Logger - Start
[elastic-2] INFO Logger - 1: 1
[elastic-3] INFO Logger - 2: 1
[elastic-3] INFO Logger - 2: 2
[elastic-2] INFO Logger - 1: 2
[elastic-3] INFO Logger - 2: 3
[elastic-2] INFO Logger - 1: 3
[elastic-2] INFO Logger - 1: 4
[elastic-3] INFO Logger - 2: 4
[elastic-2] INFO Logger - 1: 5
[elastic-3] INFO Logger - 2: 5
```

```kotlin
val logger = LoggerFactory.getLogger("Logger")
logger.info("Start")

val flux = Flux.fromIterable(1..5)
    .handle<Int> { num, sink ->
        runBlocking { delay(100L) }
        sink.next(num)
    }


flux.subscribeOn(Schedulers.parallel())
    .subscribe { logger.info("1: $it") }
flux.subscribeOn(Schedulers.parallel())
    .subscribe { logger.info("2: $it") }

runBlocking { delay(10000) }
```

```
[main] INFO Logger - Start
[parallel-2] INFO Logger - 2: 1
[parallel-1] INFO Logger - 1: 1
[parallel-2] INFO Logger - 2: 2
[parallel-1] INFO Logger - 1: 2
[parallel-2] INFO Logger - 2: 3
[parallel-1] INFO Logger - 1: 3
[parallel-2] INFO Logger - 2: 4
[parallel-1] INFO Logger - 1: 4
[parallel-2] INFO Logger - 2: 5
[parallel-1] INFO Logger - 1: 5
```



일부 연산자들은 특정 스케쥴러를 사용하도록 되어있다. 예를 들어, `Flux.interval()` 팩토리 메서드는 매 특정 시간 동안 데이터를 방출하는 Flux를 만든다. Scheduler를 지정할 수 있으며, Scheduler가 없는 경우 `Schedulers.parallel()` 을 사용한다.

```kotlin
val intervalFlux = Flux.interval(Duration.ofMillis(300), Schedulers.newSingle("Single"))

intervalFlux.subscribe { logger.info("Subscribed, $it") }
```

```
[main] INFO Logger - Start
[Single-1] INFO Logger - Subscribed, 0
[Single-1] INFO Logger - Subscribed, 1
[Single-1] INFO Logger - Subscribed, 2
[Single-1] INFO Logger - Subscribed, 3
[Single-1] INFO Logger - Subscribed, 4
```



Reactor는 reactive chain에서 실행 상황을 전환하는 두 가지 수단을 제공한다. (**publishOn**, **subscibeOn**) 두 가지 모두 Scheduler를 사용하여 실행 컨택스트를 해당 스케줄러로 전환할 수 있다. 

















