# Reactive Transaction

Spring Framework 5버전에서 WebFlux가 지원되면서 리액티브 프로그래밍을 할 수 있다. (Reactor API 등을 이용해서) 그런데 JPA를 이용해서 RDBC에 접근해서 업데이트 연산을 하거나 하면 보통 서비스 메서드에 @Transactional 어노테이션을 사용하여 트랜잭션 범위를 메서드 전체에 대해 잡는데, @Transactional은 하나의 고정된 쓰레드에서 트랜잭션 관리를 하기 때문에 쓰레드가 바뀌는 리액티브 프로그래밍에서는 적절하지 않다고 한다. 

다음 코드는 서비스 레이어에서 회원 정보를 변경하는 코드인데 실제로 @Transactional로 트랜잭션 관리가 되지 않아 JPA에서 변경 감지로 인해 DB 업데이트가 발생하지 않는다. (트랜잭션을 관리하는 쓰레드에서 업데이트 연산이 발생하지 않기 때문이 아닐까 싶다.)

```kotlin
@Transactional
fun modify(memberNo: Int, command: ProfileModifyCommand): Mono<Unit> {
    return Mono.fromCallable {
        memberRepository.findByIdOrNull(memberNo)
            ?: throw MemberNotFoundException()
    }
        .handle<Member> { member, sink ->
            if (command.newPassword != null && !passwordEncoder.matches(command.password, member.password))
                sink.error(WrongPasswordException())
            else
                sink.next(member)
        }.flatMap {
            // 변경 
            it.modify(command, command.newPassword?.let { passwordEncoder.encode(it) })
            memberRepository.save(it)
            Mono.just(Unit)
        }
        .subscribeOn(Schedulers.elastic())
}
```

 

그럼 리액티브 프로그래밍에서는 트랜잭션 관리를 어떻게 해야하는지 알아보자. ([공식문서](https://spring.io/blog/2019/05/16/reactive-transactions-with-spring))



## Reactive @Transaction을 SpringFramework에서 지원하는가?

공식문서에서 보면 리액티브 트랜잭션 관리는 필요 없다고 한다.

> At the time our journey began, we had no reactive form of transactional integrations, so this question was simple to answer: There’s no need for reactive transaction management.



몽고DB는 multi-document 트랜잭션을 MongoDB server 4.0부터 지원하고, R2DBC가 나타나기 시작하면서, Spring에서는 Spring Data R2DBC 프로젝트를 만들고 관리하고 있다. 이 두 프로젝트는 Transaction 동작을 지원하고싶어 했고 결국 Template API에서 inTransaction(...) 메서드로 네이티브 트랜잭션을 감싸는 작업을 수행할 수 있도록 제공하고 있다.

작은 트랜잭션 단위에서 inTransaction(...) 메서드를 사용하는 것은 편한 반면에, 이 API는 Spring의 트랜잭션 지원 방식을 반영하지 않는다. Spring에서는 두 가지의 트랜잭션 관리 방법을 지원하는데 첫 번째로는 **@Transactional** 이고, 두 번째로는 **TransactionTemplate**이다.

두 가지 접근방식은 모두 트랜잭션 자원의 트랜잭션을 관리하는 **PlatformTransactionManager** 를 기반으로 한다. PlatformTransactionManager는 Spring에서 제공되는 transaction manager 구현체나 JTA에 기반한  Java EE 구현체 중 하나가 될 수 있다.

두 가지 접근법 모두 트랜잭션 상태 객체를 통과하지 않고 트랜잭션 상태 관리를 허용하는 ThreadLocal 스토리지에 트랜잭션 상태를 바인딩한다는 공통점이 있다.



## 명령형 트랜잭션 관리의 작동 방식

트랜잭션 관리는 트랜잭션 상태를 실행과 연관시키는 것이 필요하다. 명령형 프로그래밍에서는 이러한 연관을 TheadLocal 스토리지에 저장한다. (트랜잭션 상태는 Thread에 바인딩된다.) 기본적으로, 트랜잭션 코드가 컨테이너에서 호출한 동일 스레드에서 실행되어야 트랜잭션 관리가 된다. (명령형 프로그래밍에서는 동일 쓰레드에서 작동한다.)

하지만 리액티브 프로그래밍은 명령형 프로그래밍 모델과 같이 동일 쓰레드에서 작동하거나 그러지 않는다. 실행 결과를 자세히 보면 코드가 서로 다른 쓰레드에서 실행되는 것을 확인할 수 있을 것이다. 간단한 테스트와 로깅만으로도 알 수 있다.

sumOnN() 메서드와 divideTo() 메서드는 하나의 Mono 객체를 만들고 subScribeOn() 메서드에서 Scheduler를 정의했다. 그리고 각각 Mono가 성공할 때 마다 로그를 남기도록 했다.

```kotlin
class CommonTest {

    val logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun `reactive test`() {
       
        val testMono = Mono.just(5).doOnSuccess {
            logger.info("[START] RESULT = $it")
        }
            .flatMap { sumOneToN(it) }
            .flatMap { divideTo(it, divideTo = 10) }
            .subscribeOn(Schedulers.elastic())

        runBlocking {
            logger.info("REACTIVE TEST!!")
            testMono.awaitSingle()
        }
    }

    fun sumOneToN(n: Int): Mono<Int> {
        return Mono.just(n * (n + 1) / 2).doOnSuccess {
            logger.info("[sumOneToN] RESULT = $it")
        }.subscribeOn(Schedulers.elastic())
    }

    fun divideTo(n: Int, divideTo: Int): Mono<Int> {
        return Mono.just(n / divideTo).doOnSuccess {
            logger.info("[divideTo] RESULT = $it")
        }.subscribeOn(Schedulers.elastic())
    }
}
```

실행 결과는 다음과 같다.

```
13:35:57.198 [Test worker @coroutine#1] INFO com.recipt.member.CommonTest - REACTIVE TEST!!
13:35:57.530 [elastic-2] INFO com.recipt.member.CommonTest - [START] RESULT = 5
13:35:57.531 [elastic-3] INFO com.recipt.member.CommonTest - [sumOneToN] RESULT = 15
13:35:57.532 [elastic-4] INFO com.recipt.member.CommonTest - [divideTo] RESULT = 1
```

Mono객체를 subscribe하기 전에는 Test worker 쓰레드에서 실행되고, Mono 객체 생성과, 각각 flatMap으로 sumOfN(), divideTo() 메서드를 실행하면서 Mono 객체를 만들 때는 서로 다른 쓰레드에서 실행된다는 것을 알 수 있다.. (elastic-N)

리액티브 프로그래밍에서는 쓰레드 스위칭이 무작위로 발생한다. 쓰레드가 바뀌는 것 때문에 ThreadLocal에 의존하는 모든 코드들이 제대로 동작하지 않을 수 있다. 그 결과 트랜잭션 상태 객체를 항상 전달하지 않고 트랜잭션 상태를 반영하기 위해 다른 방식이 필요하다.

리액티브 프로그래밍에서 Reactor Context는 명령형 프로그래밍의 ThreadLocal과 같은 역할을 한다. 컨텍스트는 특정 실행에 대한 컨텍스트 데이터를 바인딩할 수 있다. 리액티브 프로그래밍에서, 이는 Subscription(구독)이라고 한다. Reactor의 Context는 Spring을 모든 리소스와 동기화와 함께 트랜잭션 상태를 특정 subscription에 바인딩하게 해준다. Reactor에서 사용하는 모든 리액티브 코드는 리액티브 트랜잭션에 참여할 수 있다. scalar 값(Mono, Flux가 아닌 것들?)을 반환하거나 세부 트랜잭션 정보에 접근하려는 코드는 트랜잭션에 참여하기 위해서 리액티브 타입을 사용하는 코드로 작성해야 한다. 그렇지 않다면 Context는 사용 불가능하다.



## Reactive Transaction 관리

SpringFramework 5.2 M2버전 부터, Spring은 ReactiveTransactionManager SPI를 통해 트랜잭션 관리를 지원한다. ReactiveTransactionManager는 트랜잭션 리소스를 사용하는 non-blocking reactive integration의 트랜잭션 관리 추상화 객체이다. 이 객체는 Publisher 타입을 리턴하는 reactive @Transaction 메서드와 TransactionalOperator를 사용하는 프로그램 트랜잭션 관리를 기반으로 한다.

R2DBC, reactive MongoDB를 사용해야 될듯..

```kotlin
class TransactionalService (
    private val db: DatabaseClient
) {
    @Transactional
    fun insertRows(): Mono<Void> {
        return db.execute()
      	    .sql("INSERT INTO person (name, age) VALUES('Joe', 34)")
            .fetch().rowsUpdated()
            .then(db.execute().sql("INSERT INTO contacts (name) VALUES('Joe')"))
            .then()
    }
}
```



Reactive transaction은 annotation 기반의 트랜잭션 처리가 명령형 트랜잭션과 비슷하다. 차이가 있다면 리액티브에서는 리액티브 자원 추상객체 DatabaseClient가 작동한다. 모든 트랜잭션 관리는 Spring의 트랜잭션 인터셉터와 ReactiveTransactionManager를 활용하여 백그라운드에서 수행한다. 

Spring은 적용할 트랜잭션 관리 타입(메서드 리턴타입 기반)을 구분한다.

* Publisher 타입을 리턴하는 메서드: Reactive Transaction Management
* 그 외 타입: Imperative Transaction Management



JPA나 JDBC쿼리처럼 imperative components를 사용하게 된다면 이 차이를 아는 것은 매우 중요하다. 쿼리 결과를 Publisher 타입으로 감싸면 Spring에서 imperative 방식이 아닌 reactive 방식으로 트랜잭션 관리를 할 수 있도록 신호를 보내게 된다. 





















