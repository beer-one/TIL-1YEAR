# Reactor Kafka Reference

https://projectreactor.io/docs/kafka/release/reference/



Reactor Kafka는 Reactor와 Kafka Producer/Consumer API를 기반으로 한 reactive API이다. Reactor Kafka API는 non-blocking back-pressurre를 지원하는 함수형 API를 사용하며 매우 적은 오버헤드로 Kafka로 메시지를 발행하고 Kafka로부터 메시지를 소비할 수 있도록 해준다. Reactor Kafka API는 Reactor를 사용는 애플리케이션이 메시지 버스나 스트리밍의 목적으로 Kafka를 사용하고 다른 시스템과 통합하여 종단 간 reactvie pipeline을 제공할 수 있다.



## Reactive Kafka Receiver

Kafka 토픽에 저장된 메시지는 reactive receiver인 `KafkaReceiver` 를  사용하여 소비된다. 각각의 KafkaReceiver 인스턴스는 하나의 `KafkaConsumer` 와 관련되어있다 한다. KafkaConsumer가 여러 쓰레드에서 동시에 접근할 수 없기 때문에 KafkaReceiver도 마찬가지로 thread-safe하지 않는다. 



receiver는 `reactor.kafka.receiver.ReceiverOptions` 의 receiver configuration 인스턴스로 인해 생성된다. Receiver 인스턴스 생성 이후에 RecevicerOptions가 변경한 내용들은 KafkaReceiver에서 반영되지 않는다. `bootstrap kafka brokers` 와 `de-serializer`와 같은 ReceiverOptions 프로퍼티는 기반이 되는 KafkaConsumer로 전달된다. 이 프로퍼티들은 ReceiverOptions 인스턴스에서 receiver 생성 시점에서 설정되거나 `ReceiverOptions#consumerProperty` setter 사용 시점에서 설정된다. 그러므로 토픽 구독을 포함한 모든 Reactive KafkaReceiver 설정 옵션들은 KafkaReceiver 인스턴스가 생성되기 전에 추가해둬야 한다. 



`ReceiverOptions<K, V>` 와 `KafkaReveiver<K, V>` 의 제네릭 타입은 receiver를 사용하여 소비된 consumer 레코드의 key, value 타입이다. 그리고 de-serializer는 KafkaReceiver가 생성되기 전에 무조건 ReceiverOptions 인스턴스에서 설정해야 한다.



```kotlin
val consumerProps = hashMapOf<String, Any>(
    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
    ConsumerConfig.GROUP_ID_CONFIG to "sample-group",
    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to IntegerDesializer::class.java,
    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDesializer::class.java
)

val receiverOptions = ReceiverOptions.create(consumerProps)
    .subscription(Collections.singleton(topic))
```

