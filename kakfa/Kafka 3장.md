# 카프카 프로듀서

Kafka 메시지를 쓰는 용도가 다양하고, 그에 따른 요구사항도 다양하다. 모든 메시지가 중요한지, 메시지가 일부 유실되어도 되는지, 중복 처리되도 괜찮은지 처리 대기시간을 얼마까지 허용하는지 등등.. 다양하다. 이처럼 서로 다른 용도와 요구사항은 카프카에 메시지를 쓰는 프로듀서 API를 사용하는 방법과 구성에 영향을 준다.



## 작업 처리 개요

프로듀서가 데이터를 전송할 때 내부적으로 여러 단계로 처리된다.

우선 카프카에 쓰려는 메시지를 갖는 ProducerRecord를 생성한다. ProducerRecord는 전송하려는 토픽과 값을 포함하고, 선택적으로 key와 파티션을 지정할 수 있다. 그 후 메시지 객체를 byte array로 직렬화한다. (Serializer 컴포넌트가 처리)

그 다음 해당 데이터를 파티셔너 컴포넌트에 전달한다. ProducerRecord에 특정 파티션을 지정했다면 지정된 파티션을 반환하고 아니라면 key를 기준으로 파티셔너가 하나의 파티션을 지정해준다. 파티셔너가 파티션을 지정해주면 프로듀서도 메시지가 저장될 토픽과 파티션을 알게 된다.

그 다음 같은 토픽과 파티션으로 전송될 레코드들을 모은 레코드 배치에 추가하고 별개의 쓰레드에서 그 배치를 카프카 브로커에 전달한다.

브로커는 수신된 레코디의 메시지를 처리한 후 응답을 전송한다. 이 때 메시지를 성공적으로 쓰면 RecordMetadata 객체를 반환한다. (토픽, 파티션, 메시지 오프셋을 가지고 있다.) 메시지 쓰기를 실패하면 에러를 반환하고 에러를 수신한 프로듀서는 에러를 반환하기 전에 retry를 시도할 수 있다. (이거도 설정??)

![kafka](/Users/yunseowon/Desktop/kafka.png)



## 프로듀서 구성

http://kafka.apache.org/documentation.html#producerconfigs 에 프로듀서의 구성이 나와있다. 일단 기본적인 속성과 중요한 속성들만 나열해보았다.

* bootstrap.servers: kafka broker의 host:port 목록. 최소 두 개 정도의 broker를 설정하는 것이 좋다. (안정성)
* key.serializer: 메시지 키를 직렬화하기 위해 사용되는 클래스. `org.apache.kafka.common.serialization.Serializer` 인터페이스를 구현해야 한다. 

  * ByteArraySerializer, StringSerializer, IntegerSerializer 등을 지원한다.
* value.serializer: 메시지 값을 직렬화하기 위해 사용되는 클래스. key.serializer와 동일하다.
* acks: 전송된 레코드를 수신하는 파티션 레플리카의 수를 제어한다. 이 변수는 메시지가 유실될 가능성에 큰 영향을 준다.
  * acks = 0 : 프로듀서는 브로커의 응답을 기다리지 않는다. 그렇기 때문에 브로커가 수신하지 못했다는걸 알지 못하기 때문에 메시지가 유실된다면 그에 따른 적절한 처리 (재전송 등)을 하지 못한다. 대신 응답을 기다리지 않기 때문에 처리량이 매우 높다.
  * acks = 1 : 리더 레플리카가 메시지를 받는 순간 프로듀서는 브로커로부터 성공적으로 수신했다는 응답을 받는다. 만약 리더에서 메시지를 쓸 수 없다면 프로듀서에서는 에러 응답을 받을 것이고 재전송을 할 수 있다. 또한 리더가 중단되고 해당 메시지를 복제하지 않은 레플리카가 새로운 리더로 선출된다면 메시지가 유실될 수 있다.
  * acks = all : 동기화된 모든 레플리카가 메시지를 받으면 프로듀서가 브로커의 성공 응답을 받는다. 가장 안전한 형태로 리더가 변경된다고 해도 다른 레플리카도 모두 수신받아야 하기 때문에 성공한다면 모든 레플리카가 메시지를 받는다는 보장이 되어있다. 대신 그만큼 대기시간은 길어진다.
* buffer.memory: 브로커들에게 전송될 메시지의 버퍼로 사용할 메모리의 양을 설정하는 변수. 만약 메시지들이 서버에 전달될 수 있는 것 보다 더 빠른 속도로 애플리케이션에서 전송된다면 프로듀서의 버퍼 메모리가 부족하게 될 것이다. 
* compression.type: 해당 매개변수를 설정하면 메시지를 압축해서 전송한다. 이 값은 `snappy`, `gzip`, `lz4` 중 하나로 설정하면 된다. 메시지 압축을 사용함으로써 카프카로 메시지를 전송할 때 병목현상이 생길 수 있는 네트워크와 스토리지 사용을 줄일 수 있다. 대신 애플리케이션 쪽에서 CPU 사용률이 늘어난다.
  * snappy: 구글에서 만들었으며 CPU 부담이 적고 성능이 좋고 양호한 압축률을 제공한다.
  * gzip: CPU, 압축 시간을 더 많이 잡아먹지만 압축률은 좋다.
* retries: 메시지 전송에 실패했을 때 재시도 횟수
* retry.backoff.ms: 실패 - 재시도 사이 대기시간, retries와 이 변수는 하나의 브로커가 중단될 때 처리가 복구되는 시간을 테스트한 후 설정하는 것이 좋다.
* batch.size: 같은 파티션에 쓰는 다수의 레코드가 전송될 때는 프로듀서가 그것들을 배치로 모은다. 이 변수는 각 배치에 사용될 메모리 양을 제어한다. 그리고 해당 배치가 가득 차면 모든 메시지가 전송된다. 하지만 배치가 가득 차야만 프로듀서가 브로커로 메시지를 보내는 것은 아니다. batch.size가 클 수록 메모리를 많이 사용하게 되고 작을 수록 프로듀서가 브로커로부터 더 자주 메시지를 전송해야 하는 부담이 있다.
* linger.ms: 현재의 배치를 전송하기 전 까지 기다리는 시간. 클 수록 대기시간은 길어지지만 동시 처리량은 늘어난다.
* client.id: 어떤 클라이언트에서 전송된 메시지인지 브로커에게 알려주기 위한 변수. 주로 로그 메시지와 메트릭 데이터 전송에 사용된다.
* max.in.flight.requests.per.connection: 서버의 응답(ack)을 받지 않고 프로듀서가 전송하는 메시지의 개수를 제어한다. 값이 클 수록 메모리 사용은 증가하지만 처리량은 좋아진다. 
* timeout.ms: 동기화된 레플리카들이 메시지를 인지하는 동안 브로커가 대기하는 시간
* request.timeout.ms: 데이터를 전송할 때 프로듀서가 서버의 응답을 기다리는 시간
* metadata.fetch.timeout.ms: 메타데이터를 요청할 때 프로듀서가 서버의 응답을 기다리는 시간
* max.block.ms: send() 메서드를 호출할 때 프로듀서의 전송 버퍼가 가득 차거나 partitionsFor() 메서드로 메타데이터를 요청했지만 사용할 수 없을 때 일시 중단되는 시간 -> 예외 발생
* max.request.size: 프로듀서가 전송하는 쓰기 요청의 크기를 제어한다. 전송될 수 있는 가장 큰 메시지의 크기와 프로듀서 하나의 요청으로 전송할 수 있는 메시지의 최대 개수를 모두 나타낸다. 브로커의 message.max.bytes와 값이 같도록 설정하는 것이 좋다. 그래야 브로커가 거부하는 크기의 메시지를 프로듀서가 전송하지 않을 것이기 때문이다.
* receive.buffer.bytes: 데이터를 읽을 때 소켓이 사용하는 TCP 송수신 버퍼의 크기
* send.buffer.bytes: 데이터를 쓸 때 소켓이 사용하는 TCP 송수신 버퍼의 크기



간단하게 Kotlin으로 KafkaProducer 객체를 만들어보자. (나중에는 Spring으로..)

```kotlin
object MyKafkaProducer {

    private val producer: KafkaProducer<String, String>

    init {
        val properties = mapOf(
            "bootstrap.servers" to "192.168.0.4:9092",
            "key.serializer" to "org.apache.kafka.common.serialization.StringSerializer",
            "value.serializer" to "org.apache.kafka.common.serialization.StringSerializer"
        )

        producer = KafkaProducer(properties)
    }
}
```





## Kafka에 메시지 전송하기

Kotlin으로 kafka에 메시지를 전송하는 방법을 알아보자.



### 메시지 전송 방법

먼저, kafka에 메시지를 전송하는 방법은 크게 3가지가 있다. 상황에 따라 적절한 전송방식을 선택해서 사용하면 된다.

* Fire-and-forget: 가장 간단한 방법. send() 메서드로 메시지를 전송만 하고 성공 또는 실패에 대한 조치를 하지 않는 방법이다. kafka는 가용성이 높고 실패하더라도 자동으로 재전송을 실행하지만 일부 메시지가 유실될 수도 있다.
* Synchronous send: send() 메서드를 호출하면 Future 객체가 반환된다. 그 후 Future 객체의 get() 메서드를 호출하면 작업이 완료될 때 까지 기다린 다음 브로커로부터 처리 결과가 반환된다. 이 방식은 send()가 성공적으로 수행되었는지 확인할 수 있다.
* Asynchronous send: send() 메서드 호출 후 콜백 메서드를 구현한 객체를 매개변수로 전달한다. 콜백 메서드로부터 send()가 성공적으로 수행되었는지 알 수 있다.



### Fire-and-forget

먼저 fire-and-forget 방식으로 메시지를 전송해보자.

```kotlin
val record = ProducerRecord("beer", "Heineken", "Nederland")

try {
    MyKafkaProducer.send(record)
} catch (e: Exception) {
    // BufferExhaustedException or TimeoutException - 버퍼 꽉 참
    // SerializationException - 직렬화 실패
    // InterruptException - 스레드 중단
    e.printStackTrace()
}
```

* 메시지를 전송하기 위해 ProducerRecord 객체를 만든다. beer라는 토픽에 key가 Heineken, value가 Nederland인 레코드를 생성한 후 send() 메서드를 이용하여 메시지를 전송한다.
* send() 메서드의 리턴 타입이 Future 객체인데 여기서는 반환값을 무시하기 때문에 별 다른 확인 및 조치를 하지 않는다.



### Synchronous send

```kotlin
val logger = LoggerFactory.getLogger("logger")

val record = ProducerRecord("beer", "Heineken", "Nederland")

try {
    val metadata = MyKafkaProducer.send(record)
        .get()
    logger.info("offset: ${metadata.offset()}, partition: ${metadata.partition()}")

} catch (e: Exception) {
    e.printStackTrace()
}
```

* get() 메서드를 사용해서 카프카의 응답을 기다린다. get() 메서드를 호출하면 전송된 레코드가 어느 파티션의 어느 오프셋으로 저장되는지 알 수 있다.
* 여러가지 예외가 발생할 수 있다.
  * 카프카에 메시지를 전송하기 전에 에러 발생
  * 전송 중에 카프카 브로커가 재시도 불가능 에러를 반환
  * 재시도 횟수 초과



카프카 프로듀서를 사용할 때는 재시도 가능한 에러와 불가능한 에러가 있다. 재시도 가능한 에러는 여러가지가 있는데 대표적으로 이런 것들이 있다.

* connection error (브로커와 연결이 갑자기 안될 때)
* no leader (새로운 리더가 선출되면 재시도 가능)

재시도가 불가능한 에러는 대표적으로 이런 것이 있다.

* 메시지 크기가 너무 클 때



### Asynchronous send

보통 애플리케이션에서는 전송 성공했을 때 반환되는 메타데이터가 필요 없다. (전송만 되면 되기 때문) 하지만 실패했을 때는 에러 처리나 로깅을 위해 메타데이터가 필요할 수도 있다.

Callback 인터페이스를 구현하여 콜백을 추가할 수도 있지만 예제에서는 람다를 사용해서 구현하였다.

```kotlin
fun asynchronousSend() {
    val record = ProducerRecord("beer", "Terra", "Korea")

    try {
        val metadata = MyKafkaProducer.send(record) { recordMetadata, exception ->
            exception?.printStackTrace()

            recordMetadata?.let{
                logger.info("offset: ${it.offset()}, partition: ${it.partition()}")
            }
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }

    Thread.sleep(1000)
}
```

* 콜백은 비동기이기 때문에 sleep을 두었다. 프로세스가 죽는다면 메시지 전송이 안되기 때문

  

## 파티션

카프카 레코드는 토픽 이름과 key, value로 이루어져있는데 key는 있을 수도 있고 없을 수도 있다. key는 두 가지 목적으로 사용되는데 하나는 메세지를 식별하기 위함이고, 다른 하나는 파티션을 결정하기 위함이다. 

key가 null이면서 기본 파티셔너가 사용될 때에는 사용 가능한 토픽의 파티션 중 하나가 무작위로 선택되어 해당 레코드가 저장된다. 그리고 각 파티션에 저장되는 메시지 개수의 균형을 맞추기 위해 라운드로빈 알고리즘이 사용된다. key의 값이 있다면서 기본 파티셔너가 사용된다면 카프카의 키의 해시값을 구한 후 그 값에 따라 특정 파티션에 메시지를 저장한다. 