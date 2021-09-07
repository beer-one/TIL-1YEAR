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