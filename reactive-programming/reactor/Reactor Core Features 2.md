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



##Threading and Schedulers

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



### publishOn

publishOn은 subscriber chain의 중간에 다른 operator와 동일한 방식으로 적용된다. 관련된 스케쥴러의 worker에 대해 콜백을 실행하는 동안 upstream에서 신호를 가져와 downstream으로 재생한다. 동시에, 후속 연산자가 실행되는 위치에 영향을 미친다.

* Scheduler에 의해 선택된 쓰레드로 실행 컨텍스트가 변경된다.
* 명세에 따라 onNext call이 순서대로 발생하므로 단일 쓰레드를 사용한다.
* 특정 스케쥴러에서 작동하지 않는다면, publishOn 이후의 operator는 계속해서 같은 쓰레드를 사용한다.



```kotlin
fun main() {
    val logger = LoggerFactory.getLogger("Thread")
    val schedulers = Schedulers.newParallel("parallel", 4)

    val flux = Flux.range(1, 2)
        .map {
            logger.info("${it + 10}")				// [main] INFO Thread - 11
            it + 10
        }
        .publishOn(schedulers)							// 컨텍스트 전환 (main -> parallel-x)
        .map {
            logger.info("value $it")				// [parallel-x] INFO Thread - value 11
            "value $it"
        }

    Thread { 
        flux.subscribe { logger.info(it) }	// [parallel-x] INFO Thread - value 11
    }.run()

    runBlocking { delay(10000L) }
}
```

* publishOn() 이전과 이후 체인은 다른 쓰레드를 사용한다.
* publishOn()을 호출하면 스케쥴러에 의해 실행 컨텍스트가 변경된다.



### subscribeOn

subscribeOn은 backward chain이 구성될 때 구독 프로세스에 적용된다. 그리고 어느 체인에서 subscribeOn()을 호출함과 관계 없이, 항상 source 방출의 context에 영향을 끼친다. 그러나 publishOn에 대한 후속 호출의 동작에는 영향을 미치지 않으며, 이후 체인에 대한 실행 컨텍스트를 계속 전환한다.

* subscribeOn의 Scheduler에 의해 선택된 쓰레드로 전체 체인에 대한 실행 컨텍스트가 변경된다.
* publishOn 이후의 체인에 대해서는 publishOn의 Scheduler에 의해 선택된 쓰레드로 실행 컨텍스트가 변경된다.



```kotlin
fun main() {
    val logger = LoggerFactory.getLogger("Thread")
    val schedulers = Schedulers.newParallel("parallel", 4)

    val flux = Flux.range(1, 2)
        .subscribeOn(Schedulers.boundedElastic())		// 일단 boundedElastic-x 로 컨텍스트 전환
        .map {
            logger.info("${it + 10}")								// [boundedElastic-x] INFO Thread - 11
            it + 10
        }
        .publishOn(schedulers)											// 이후 체인은 parallel-x 로 전환
        .map {
            logger.info("value $it")								// [parallel-x] INFO Thread - value 11
            "value $it"
        }

    Thread {
        flux.subscribe { logger.info(it) }					// [parallel-x] INFO Thread - value 11
    }.run()

    runBlocking { delay(10000L) }
}
```





## 에러 핸들링

리액티브 스트림에서 에러는 이벤트를 종료시킨다. 에러가 발생하면 시퀀스가 종료되고 연산자 체인을 따라 마지막 단계의 체인과, 정의한 구독자 및 해당 onError 메서드로 전파된다.

애플리케이션 단에서 정의되는 에러도 있기 마련인데, 이러한 에러를 위해서 subscriber의 `onError` 메서드를 정의하여 에러를 핸들링할 수 있다. onError 메서드를 정의하지 않는다면 UnsupportedOperationException이 던져진다. 

Reactor는 chain의 중간에서 error-handling operator로 에러를 핸들링하게 할 수도 있다. operator로는 아래 역할을 할 수 있다.

* 에러를 잡고 default value 리턴
* 에러를 잡고 fallback method 실행
* 에러를 잡고 fallback value 계산
* 에러를 잡고 다른 Exception 방출
* 에러를 잡고 로깅 후 기존 Exception 방출
* finally block과 같은 역할 하기



### onErrorReturn

onErrorReturn은 에러를 잡고 default value를 리턴시킨다. 에러가 발생하면 default value를 리턴하고 구독이 종료된다.

```kotlin
fun main() {
    val logger = LoggerFactory.getLogger("Logger")

    val flux = Flux.fromIterable(1..10)
        .handle<Int> { num, sink ->
            if (num == 4) sink.error(NumberIs4Exception())
            else sink.next(num)
        }.onErrorReturn(-1)


    flux.subscribe { logger.info(it.toString()) }
}
```

```
[main] INFO Logger - 1
[main] INFO Logger - 2
[main] INFO Logger - 3
[main] INFO Logger - -1
```



### onErrorResume

onErrorResume은 onErrorReturn보다 더 일반적인 방법으로 에러를 핸들링하는 방법이다. fallback function의 리턴값을 onErrorResume의 에러핸들링 리턴 값으로 사용한다.



```kotlin
fun main() {
    val logger = LoggerFactory.getLogger("Logger")

    val fallback = Mono.fromCallable {
        Random.nextInt(-10, -1)
    }

    val flux = Flux.fromIterable(1..10)
        .handle<Int> { num, sink ->
            if (num == 4) sink.error(NumberIs4Exception())
            else sink.next(num)
        }.onErrorResume { fallback }

    flux.subscribe { logger.info(it.toString()) }
}
```

```
[main] INFO Logger - 1
[main] INFO Logger - 2
[main] INFO Logger - 3
[main] INFO Logger - -2
```



onErrorReturn과 onErrorResume은 파라미터 `Class<E> type` 을 지정하여 특정 예외만을 받아서 처리하도록 설정할 수도 있다. 

```kotlin
fun main() {
    val logger = LoggerFactory.getLogger("Logger")

    val fallback = Mono.fromCallable {
        Random.nextInt(-10, -1)
    }

    val flux = Flux.fromIterable(5..10)
        .handle<Int> { num, sink ->
            if (num == 4) sink.error(NumberIs4Exception())
            else if (num == 5) sink.error(NumberIs5Exception())
            else sink.next(num)
        }.onErrorResume(NumberIs4Exception::class.java) { fallback }
        .onErrorReturn(NumberIs5Exception::class.java, 0)
    
    flux.subscribe { logger.info(it.toString()) }
}
```

```
[main] INFO Logger - 0
```



### onErrorMap

onErrorMap은 에러를 받아서 다른 에러로 다시 던지는 역할을 한다. NumberIs4Exception이 발생한 후 onErrorMap을 지나면 WrongNumberException()으로 다시 던져지는걸 확인할 수 있다.

```kotlin
fun main() {
    val logger = LoggerFactory.getLogger("Logger")
  
    val flux = Flux.fromIterable(1..10)
        .handle<Int> { num, sink ->
            if (num == 4) sink.error(NumberIs4Exception())
            else sink.next(num)
        }.onErrorMap(NumberIs4Exception::class.java) { WrongNumberException() }
        .onErrorResume {
            logger.error("Error: ", it)
            Mono.just(0)
        }

    flux.subscribe { logger.info(it.toString()) }
}
```

```
[main] INFO Logger - 1
[main] INFO Logger - 2
[main] INFO Logger - 3
[main] ERROR Logger - Error: 
com.floidea.example.WrongNumberException
	...
```



### doOnError

doOnError는 에러가 발생할 때 그걸 캐치하고 로깅을 하는 등 다른 액션을 취한 후 해당 에러를 다시 던지는 역할을 한다. doOnError를 거쳐 에러로깅을 한 후 에러가 다시 던져저서 onErrorReturn에 걸린다.

```kotlin
fun main() {
    val logger = LoggerFactory.getLogger("Logger")

    val flux = Flux.fromIterable(1..10)
        .handle<Int> { num, sink ->
            if (num == 4) sink.error(NumberIs4Exception())
            else sink.next(num)
        }.doOnError {
            logger.error("Error occurred", it)
        }
        .onErrorReturn(NumberIs4Exception::class.java, 0)

    flux.subscribe { logger.info(it.toString()) }
}
```

```
[main] INFO Logger - 1
[main] INFO Logger - 2
[main] INFO Logger - 3
[main] ERROR Logger - Error occurred
com.floidea.example.NumberIs4Exception
...
[main] INFO Logger - 0
```





### doFinally

에러와는 관계 없지만 에러가 발생함과 관계없이 액션을 취할 때 사용한다. 에러 발생, 취소 되어 스트림이 끝나거나 정상적으로 스트림이 완료되었을 때 모두 doFinally를 실행한다. doFinally 블록은 리소스를 해제하는 작업을 할 때 유용하다.

```kotlin
fun main() {
    val logger = LoggerFactory.getLogger("Logger")
    
    val flux = Flux.fromIterable(1..3) // 1..4로 변경하면..
        .handle<Int> { num, sink ->
            if (num == 4) sink.error(NumberIs4Exception())
            else sink.next(num)
        }.doOnComplete() { logger.info("Completed!") }
        .doOnError { logger.info("Error!") }
        .doFinally { logger.info("Finally!") }

    flux.subscribe { logger.info(it.toString()) }
}
```

**에러 발생 안할 경우**

```
[main] INFO Logger - 1
[main] INFO Logger - 2
[main] INFO Logger - 3
[main] INFO Logger - Completed!
[main] INFO Logger - Finally!
```

**에러 발생 할 경우**

```
[main] INFO Logger - 1
[main] INFO Logger - 2
[main] INFO Logger - 3
[main] INFO Logger - Error!
[main] INFO Logger - Finally!
```





### Retrying

에러가 발생하면 onErrorXXX 또는 doOnError를 사용하여 에러 핸들링을 하거나 로깅 등 후속 처리를 할 수 있다. 하지만, 에러가 발생하면 그 즉시 Flux(Mono)는 종료되는데, 에러가 발생하는 이유가 일시적인 오류여서 재 시도를 하면 해결할 수 있는 상황도 분명 있을 것이다. (재시도를 하는 것이 에러 핸들링의 최선의 방법인 경우도 있다.) 

Reactor에서는 retry() 라는 메서드를 이용하여 스트림 구독 재시도를 할 수 있다. retry()의 파라미터는 시도 횟수이며 재시도를 하게되면 첫 번째 요소부터 구독을 다시 시도한다.

아래 로그를 보면 에러 발생 후 `Temporary error ocuurred!!` 라는 로그를 쌓은 후 처음부터 다시 시도하는 것을 알 수 있는데 에러가 한 번 더 발생하면 retry를 하지 않고 즉시 구독이 종료된다.



```kotlin
fun main() {
    val logger = LoggerFactory.getLogger("Logger")

    val flux = Flux.interval(Duration.ofMillis(100L))
        .handle<Long> { num, sink ->
            if(Random.nextInt(10) == 0) sink.error(TemporaryException())
            else sink.next(num)
        }.doOnError { logger.error("Temporary error occurred!!") }
        .retry(1)
        .doFinally { logger.info("Finished") }

    flux.subscribe { logger.info(it.toString()) }
}
```

```
[parallel-1] INFO Logger - 0
[parallel-1] INFO Logger - 1
[parallel-1] ERROR Logger - Temporary error occurred!!
[parallel-2] INFO Logger - 0
[parallel-2] INFO Logger - 1
[parallel-2] INFO Logger - 2
[parallel-2] INFO Logger - 3
[parallel-2] INFO Logger - 4
[parallel-2] INFO Logger - 5
[parallel-2] ERROR Logger - Temporary error occurred!!
[parallel-2] INFO Logger - Finished
```





retry 메서드보다 더 발전된 버전의 메서드인 `retryWhen()` 도 있다. retryWhen은 Companion Flux를 사용하여 어떤 실패 상황에서는 retry를 할지 말지 정할 수 있다. 이 Companion Flux는 retry 전략을 담고있는 `Flux<RetrySignal>` 이며, 이는 retryWhen의 매개변수로 설정된다. 

Retry 클래스는 추상 클래스이지만, 단순 람다로 companion을 변환하려는 경우에 Factory method를 제공한다. Retry cycle은 다음 단계로 진행된다.

1. 에러가 발생한 시간마다, companion flux로 RetrySignal이 방출된다. RetrySignal은 오류와 오류와 관련된 메타데이터의 접근을 제공한다.
2. companion Flux가 값을 방출하면 재시도를 한다.
3. companion Flux가 완료되면 에러가 발생할 때 재시도를 하지 않고 시퀀스를 종료시킨다.
4. companion Flux가 에러를 발생한다면 재시도를 하지 않고 시퀀스를 종료시킨다.



```kotlin
fun main() {
    val logger = LoggerFactory.getLogger("Logger")

    val flux = Flux.interval(Duration.ofMillis(100L))
        .handle<Long> { num, sink ->
            if(Random.nextInt(10) == 0) sink.error(TemporaryException())
            else sink.next(num)
        }.doOnError { logger.error("Temporary error occurred!!") }
        .retryWhen(Retry.from { it.take(3) })
        .doFinally { logger.info("Finished") }

    flux.subscribe { logger.info(it.toString()) }

    runBlocking { delay(10000L) }
}
```

```
[parallel-1] INFO Logger - 0
[parallel-1] INFO Logger - 1
[parallel-1] INFO Logger - 2
[parallel-1] INFO Logger - 3
[parallel-1] ERROR Logger - Temporary error occurred!!
[parallel-2] INFO Logger - 0
[parallel-2] INFO Logger - 1
[parallel-2] ERROR Logger - Temporary error occurred!!
[parallel-3] INFO Logger - 0
[parallel-3] INFO Logger - 1
[parallel-3] ERROR Logger - Temporary error occurred!!
[parallel-3] INFO Logger - Finished
```





















