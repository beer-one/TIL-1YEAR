# Reactor Core Features

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

