# 카프카 컨슈머

카프카로부터 데이터를 읽는 애플리케이션은 KafkaConsumer를 사용해서 카프카 토픽을 읽고 메시지를 받는다. 



## 컨슈머 개념

카프카로 데이터를 읽는 방법을 이해하기 위해서는 컨슈머와 컨슈머 그룹을 알아야 한다.



### 컨슈머와 컨슈머 그룹

컨슈머들은 컨슈머 그룹에 속한다. 그리고 다수의 컨슈머가 같은 토픽을 소비하면서 같은 컨슈머 그룹에 속할 때는 각 컨슈머가 해당 토픽의 서로 다른 파티션을 분담해서 메시지를 읽을 수 있다.  

컨슈머 그룹의 컨슈머 개수와 토픽의 파티션 개수에 따른 컨슈머와 파티션의 연결 구조를 파악해보자. 먼저 4개의 파티션을 갖는 토픽 T1이 있고 그 토픽을 구독하는 컨슈머 C1은 컨슈머 그룹 G1에 속해있다고 하자. 이 경우에는 C1 컨슈머는 T1토픽의 4개의 파티션 모두에 있는 메시지를 읽는다.

![image](/Users/yunseowon/Library/Application Support/typora-user-images/image-20210221141729569.png)

만약 G1 컨슈머 그룹에 컨슈머가 하나 더 추가된다면 각 컨슈머는 두 개의 파티션에서만 메시지를 읽으면 된다.

![image-20210221143209689](/Users/yunseowon/Library/Application Support/typora-user-images/image-20210221143209689.png)

만약 컨슈머 개수와 파티션의 개수가 같다면 각 컨슈머는 하나의 파티션에서만 메시지를 읽으면 된다.

![image-20210221143359555](/Users/yunseowon/Library/Application Support/typora-user-images/image-20210221143359555.png)

토픽 개수보다 컨슈머 개수가 많다면 노는 컨슈머가 생길 것이다.

![image-20210221143515362](/Users/yunseowon/Library/Application Support/typora-user-images/image-20210221143515362.png)

하나의 컨슈머 그룹에 더 많은 컨슈머를 추가하면 카프카 토픽의 데이터 소비를 확장할 수 있다. 보통 이런 방법은 대기 시간이 긴 작업을 수행하는 카프카 컨슈머에 많이 사용된다. 하나의 컨슈머로는 토픽의 데이터 추가 속도를 따라 잡을 수 없기 때문이다. 그러나 토픽의 파티션 개수보다 더 많은 컨슈머를 추가하는 것은 의미가 없다는 것을 알아두자. (노는 컨슈머가 발생하기 때문에)



한 애플리케이션의 데이터 소비를 확장하기 위해 컨슈머를 추가하는 것과 더불어 같은 토픽의 데이터를 다수의 애플리케이션이 읽어야 하는 경우도 많다. (ex. 회원 가입 토픽이 발행되면 웰컴 쿠폰 지급하는 컨슈머와 회원 가입 완료 메일을 전송하는 컨슈머가 해당 토픽을 동시에 읽어야 함.) 같은 토픽의 데이터를 다수의 애플리케이션이 읽어야 하는 경우, 각 애플리케이션이 토픽의 일부 메시지가 아닌 모든 메시지를 읽어야 한다. 그리고 이렇게 하려면 각 애플리케이션이 자신의 컨슈머 그룹을 갖도록 해야 한다. (각 컨슈머는 스레드로 구현되어 병렬적으로 실행한다.)

앞의 예시에서 G1과는 다른 컨슈머 그룹 G2를 생성하면 G1그룹과는 무관하게 T1토픽의 모든 메시지를 읽는다. 

![image-20210221144449009](/Users/yunseowon/Library/Application Support/typora-user-images/image-20210221144449009.png)



### 컨슈머 그룹과 리밸런싱

컨슈머 그룹의 컨슈머들은 자신들이 읽는 토픽 파티션의 소유권을 공유한다. 그리고 새로운 컨슈머를 그룹에 추가하면 이전 다른 컨슈머가 읽던 파티션의 메시지들을 읽는다. 특정 컨슈머가 문제가 생겨 중단될 때도 마찬가지로 중단된 컨슈머가 읽고 있던 파티션을 다른 컨슈머가 재할당받아 읽는다. 이런 식으로 한 컨슈머로부터 다른 컨슈머로 파티션 소유권을 이전하는 것을 리밸런싱이라고 한다. 

리밸런싱은 컨슈머 그룹의 가용성과 확장성을 높여준다. 대신에 리밸런싱을 처리하는 동안에는 컨슈머들은 메시지를 읽을 수 없으므로 해당 컨슈머 그룹 전체가 잠시나마 사용 불가능 상태가 된다. 또한 한 컨슈머로부터 다른 컨슈머로 파티션이 이전될 때는 해당 컨슈머의 이전 파티션에 관한 현재 상태 정보가 없어진다. 캐시 메모리에 있던 데이터도 지워지기 때문에 컨슈머의 해당 파티션 상태 정보가 다시 설정될 때 까지 애플리케이션 실행이 느려질 수 있다. 



Group Coordinator로 지정된 카프카 브로커에게 컨슈머가 heartbeat(살아있는지 알려주는 신호)를 전송하면 자신이 속한 컨슈머 그룹의 멤버십과 자신에게 할당된 파티션 소유권을 유지할 수 있다. heartbeat는 컨슈머가 폴링할 때 또는 읽은 메시지를 커밋할 때 자동 전송된다. 

만약 컨슈머가 세션 타임아웃 시간이 경과할 때 까지 heartbeat 전송을 중단하거나 컨슈머에 문제가 생겨 중단되면 GroupCoordinator가 해당 컨슈머에 문제가 생겨 중단된 것으로 간주하고 리밸런싱을 시작시킨다. 그리고 그 동안은 중단된 컨슈머가 소유한 파티션의 메시지가 처리되지 않는다. 컨슈머가 정상종료 된다면 GroupCoordinator에게 떠난다는 것을 알려주면 되며 이 때 GroupCoordinator는 처리 공백을 줄이기 위해 바로 리밸런싱을 시작한다. 





## 컨슈머 생성하기

레코드의 소비를 시작하려면 컨슈머 클래스인 KafkaConsumer 인스턴스를 생성해야 한다. 



### 속성

컨슈머를 운영하기 위해 사용하는 중요한 속성들을 정리해보았다.

* bootstrap.servers: kafka broker의 host:port 목록. 
* key.deserializer: 메시지 키를 역직렬화하기 위해 사용되는 클래스. `org.apache.kafka.common.serialization.Deserializer` 인터페이스를 구현해야 한다. 

  * ByteArrayDeserializer, StringDeserializer, IntegerDeserializer 등을 지원한다.
* value.deserializer: 메시지 값을 직렬화하기 위해 사용되는 클래스. key.serializer와 동일하다.

* group.id: 컨슈머가 속하는 컨슈머 그룹을 나타낸다. 
* fetch.min.bytes: 브로커로부터 레코드를 가져올 때의 데이터의 최소 바이트 수. 브로커에서 지정된 양 보다 적게 레코드를 보유하면 지정된 양 이상이 될 때 까지 기다렸다가 컨슈머에게 전송한다. 
* fetch.max.wait.ms: 브로커로부터 레코드를 가져오는 주기(?) default = 500ms fetch.min.bytes 값에 관계없이 컨슈머는 데이터를 가져 온 후 최대 해당 값 만큼의 시간동안 기다린 후 다시 데이터를 가져온다.
* max.partition.fetch.bytes: 서버가 파티션당 반환하는 최대 바이트 수
* session.time.out.ms: 컨슈머가 브로커와 연결이 끊기는 시간. default = 10초. 컨슈머가 `session.time.out.ms` 동안 하트비트를 전송하지 않으면 이 컨슈머는 실행이 종료되는 것으로 간주하고 리밸런싱을 시작시킨다.
* auto.offset.reset: 커밋된 오프셋이 없는 파티션을 컨슈머가 읽기 시작할 때 또는 커밋된 오프셋이 있지만 유효하지 않을 때, 컨슈머가 어떤 레코드를 읽게 할 것인지를 제어하는 변수.
  * latest: 가장 최근의 레코드들을 읽는다.
  * earliest: 파티션의 맨 앞부터 모든 데이터를 읽는다.
* enable.auto.commit: 컨슈머의 오프셋 커밋을 자동으로 할 것인지 제어한다. default = true
* partition.assignment.strategy: 토픽의 파티션들이 컨슈머 그룹의 각 컨슈머에게 할당시키는 것을 처리하는 클래스 이름을 지정한다. 전략은 두 가지가 있다.
  * Range (default): 컨슈머들이 구독하는 모든 토픽의 파티션들을 각 컨슈머마다 연속적으로 할당한다. 토픽 파티션 = (T10, T11, T12, T20 T21, T21), 컨슈머 = (C1, C2)라고 간주하면 C1 -> (T10, T11, T20, T21), C2 -> (T12, T22) (=`org.apache.kafka.clients.consumer.RangeAssignor` )
  * RoundRobin: 구독하는 모든 토픽의 모든 파티션들을 컨슈머들에게 하나씩 번갈아 차례대로 할당한다. C1 -> (T10, T12, T21), C2 -> (T11, T20, T22)(=`org.apache.kafka.clients.consumer.RoundRobinAssigner`) 
* client.id: 클라이언트로부터 전송된 메시지를 식별하기 위해 브로커가 사용
* max.poll.records: 한 번의 poll() 메서드 호출에서 반환되는 레코드의 최대 개수를 제어한다.
* receive.buffer.bytes: 데이터를 읽을 때 소켓이 사용하는 TCP 버퍼의 크기
* send.buffer.bytes: 데이터를 쓸 때 소켓이 사용하는 TCP 버퍼의 크기





### 컨슈머 생성

간단하게 Kotlin으로 KafkaConsumer 객체를 만들어보자. (나중에는 Spring으로..)

```kotlin
class MyKafkaConsumer(
    private val groupId: String,
    private val consumerNo: Int,
    topics: List<String>
) {
    private val consumer: KafkaConsumer<String, String>
    private val customerCountryMap: MutableMap<String, Int> = mutableMapOf()

    companion object {
        private val TIMEOUT = Duration.ofMillis(100)
        private val logger = LoggerFactory.getLogger(javaClass)
        private var sequence = AtomicInteger(0)

        fun create(groupId: String, topics: List<String>) = MyKafkaConsumer(
            groupId = groupId,
            consumerNo = sequence.getAndIncrement(),
            topics = topics
        )
    }

    init {
        val properties = mapOf(
            "bootstrap.servers" to "192.168.0.4:9092",
            "key.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
            "value.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
            "group.id" to groupId
        )

        consumer = KafkaConsumer(properties)
        consumer.subscribe(topics)
    }

    fun consume() {
        consumer.use { consumer ->
            while (true) {
                consumer.poll(TIMEOUT).forEach {
                    process(it)
                }
            }
        }
    }

    private fun process(record: ConsumerRecord<String, String>) {
        logger.info("[Consumer $consumerNo] Topic: ${record.topic()}, partition = ${record.partition()}, offset = ${record.offset()}, key = ${record.key()}, value = ${record.value()}")

        val updateCount = (customerCountryMap[record.value()]?: 0) + 1
        customerCountryMap[record.value()] = updateCount

        logger.info("[Consumer $consumerNo] Current: $customerCountryMap")
    }
}
```



컨슈머를 생성한 다음에는 하나 이상의 토픽을 구독해야 한다. subscribe() 메서드로 구독할 수 있으며 이 메서드는 토픽 이름을 저장한 List를 매개변수로 받는다.

```kotlin
consumer.subscribe(Collections.singletonList("consumerCountries"))
```

또한 정규 표현식을 매개변수로 전달하여 subscribe() 메서드를 호출할 수도 있다.

```kotlin
consumer.subscribe(Pattern.compile("test.*"))
```



### 폴링

MyKafkaConsumer 객체에서 consume() 메서드가 있다. 이 메서드에서 카프카 브로커로부터 토픽을 지속적으로 읽어 데이터를 읽는 데 필요한 모든 작업을 처리한다. 작업 처리는 process() 메서드에서 진행한다.

* use 라는 스코프를 사용하였다. 이 스코프 함수는 Closeable 인터페이스 객체에서 제공되는 함수인데 에러가 발생하면 자동적으로 close() 메서드를 호출한다. 컨슈머가 종료될 때는 항상 close() 메서드를 실행해야 하는데 이 때 네트워크 연결과 소켓을 닫는다. 그리고 곧바로 리밸런싱도 수행된다. close()를 호출하지 않는다면 GroupCoordinator가 해당 컨슈머가 종료된다는 것을 알 때 까지 기다리게 되어 다른 컨슈머들이 더 오랫동안 종료된 컨슈머가 읽고 있는 파티션의 메시지를 읽을 수 없게 된다.
* 폴링은 지속적으로 데이터를 읽기 때문에 무한루프 내에서 진행된다.
* poll() 메서드에서 브로커로부터 여러 개의 카프카 레코드를 가져온다. 이 메서드에서 파라미터로 timeout을 지정할 수 있다. timeout이 0이 아닐 때는 읽을 데이터가 컨슈머 버퍼에 없을 때 브로커로부터 데이터가 도착하기를 지정된 시간만큼 기다린다.
* forEach 스코프 내에서 각 레코드를 처리하는 기능을 작성하면 된다.



### 컨슈머 실행

하나의 스레드에서는 같은 그룹에 속하는 다수의 컨슈머를 함께 실행시킬 수 없으며 다수의 쓰레드가 같은 컨슈머를 안전하게 사용할 수도 없다. 즉, 한 스레드당 하나의 컨슈머를 사용하는 것이 원칙이다. 각 컨슈머를 별개의 스레드로 실행하려면 컨슈머를 하나의 클래스로 정의한 후 자바의 ExecutorService를 사용하여 스레드로 실행시키는 것이 좋다.

```kotlin
fun main() {
    val executor = Executors.newFixedThreadPool(ConsumerConfig.CONSUMER_NUM)

    val consumers = (1..ConsumerConfig.CONSUMER_NUM).map {
        MyKafkaConsumer.create(ConsumerConfig.GROUP_ID, ConsumerConfig.TOPICS)
    }

    consumers.forEach { consumer ->
        executor.submit {
            consumer.consume()
        }
    }

    executor.shutdown()
}

object ConsumerConfig {
    val TOPICS = listOf("beer")
    const val GROUP_ID = "BEER_GROUP"
    const val CONSUMER_NUM = 4
}
```

* CONSUMER_NUM으로 컨슈머의 개수를 정한 후 Executors 객체를 사용하여 컨슈머 개수만큼의 스레드풀을 생성한다.

* MyKafkaConsumer.create() 메서드로 컨슈머 개수만큼 컨슈머를 생성한다. 정적 변수 sequence를 이용하여 consumerNo를 생성 시 부여한다.

  ```kotlin
  class MyKafkaConsumer(
      private val groupId: String,
      private val consumerNo: Int,
      topics: List<String>
  ) {
      private val consumer: KafkaConsumer<String, String>
      private val customerCountryMap: MutableMap<String, Int> = mutableMapOf()
  
      companion object {
          private val TIMEOUT = Duration.ofMillis(100)
          private val logger = LoggerFactory.getLogger(javaClass)
          private var sequence = AtomicInteger(0)
  
          fun create(groupId: String, topics: List<String>) = MyKafkaConsumer(
              groupId = groupId,
              consumerNo = sequence.getAndIncrement(),
              topics = topics
          )
      }
      ...
  }
  ```

  



### 커밋과 오프셋

Kafka에서 파티션 내부의 현재 위치를 변경하는 것을 커밋이라고 한다. 컨슈머가 오프셋을 커밋하면 카프카는 내부적으로 `__consumer_offset` 이라는 이름의 특별한 토픽에 메시지를 쓴다. 이 토픽은 모든 컨슈머의 오프셋을 갖는다.  컨슈머 그룹의 모든 컨슈머들이 정상적으로 실행 중일 때는 오프셋을 커밋해도 아무런 영향을 주지 않는다. 그러나 기존 컨슈머가 비정상적으로 종료되거나 새로운 컨슈머가 컨슈머 그룹에 추가된다면 오프셋 커밋은 리밸런싱을 유발한다. 

만약 마지막으로 커밋된 오프셋이 컨슈머가 가장 최근에 읽고 처리한 메시지의 오프셋보다 작으면 마지막으로 커밋된 오프셋과 최근에 읽고 처리된 오프셋 사이의 메시지들이 두 번 처리된다.

![image-20210221220159734](/Users/yunseowon/Library/Application Support/typora-user-images/image-20210221220159734.png)

마지막으로 커밋된 오프셋이 컨슈머가 가장 최근에 읽고 처리한 오프셋보다 크면, 최근에 처리한 오프셋과 마지막으로 커밋된 오프셋 사이의 메시지들이 컨슈머 그룹에서 누락된다.

![image-20210221220251181](/Users/yunseowon/Library/Application Support/typora-user-images/image-20210221220251181.png)





### 자동 커밋

자동 커밋은 가장 쉬운 오프셋 커밋 방법이다. `enable.auto.commit` = true로 설정하면 poll() 메서드에서 받은 오프셋 중 가장 큰 것을 `auto.commit.interval.ms` 초 마다 한 번씩 커밋한다. (Default = 5초) 자동 커밋이 활성화된 상태에서 poll() 메서드를 호출하면 항상 이전 호출에서 반환된 마지막 오프셋을 커밋한다. 따라서 그 이후로 읽고 처리했던 메시지가 어떤 것인지는 모른다. 그러므로 poll() 메서드에서 반환된 모든 메시지는 다시 poll()을 호출하기 전에 처리가 끝나도록 하는 것이 중요하다. 



### 수동 커밋

`enable.auto.commit`=false로 설정하면 애플리케이션에서 요구할 때만 오프셋이 커밋된다. commitSync() 메서드로 오프셋을 수동 커밋할 수 있는데 이 메서드는 poll() 메서드에서 반환된 마지막 오프셋을 커밋한다. 커밋에 실패하면 예외를 발생시킨다.

```kotlin
while (true) {
    consumer.poll(TIMEOUT).forEach {
        process(it)
    }
    
    try {
        consumer.commitSync()
    } catch (e: CommitFailedException) {
        logger.error("$consumerName Commit Failed!", e)
    }
}
```



commitSync()는 동기식 커밋이다. 동기식 커밋의 단점은 브로커가 커밋 요청에 응답할 때 까지 애플리케이션이 일시 중지되어 처리량이 줄어든다는 것이다. 물론 commitAsync() 메서드를 통해 비동기 방식으로 커밋을 할 수도 있다.

```kotlin
while (true) {
    consumer.poll(TIMEOUT).forEach {
        process(it)
    }
    
    consumer.commitAsync { offset, e ->
     		logger.error("$consumerName Commit failed for offset $offset", e)
    }
}
```

비동기 방식은 애플리케이션이 중지되어 처리량이 줄어드는 것은 없어지지만, 커밋에 실패했을 경우 커밋을 재시도하는 것이 불가능하다는 점이 단점이다. 그리고 커밋 순서가 꼬여버리면 레코드의 중복 처리가 발생한다.

