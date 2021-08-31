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

















