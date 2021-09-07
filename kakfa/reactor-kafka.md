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



옵션 인스턴스에서 필수 옵션을 설정한 후에 메시지를 소비하기 위해 이 옵션을 사용하여 새로운 KafkaReceiver 인스턴스를 생성할 수 있다. 아래에 receiver 인스턴스를 생성하고 receiver에 inbounded Flux를 생성하는 코드가 있다. 

```kotlin
val inboundFlux: Flux<ReceiverRecord<Int, String>> = KafkaReceiver.create(receiverOptions)
    .receive();
```

 

Inbounded Kafka Flux는 토픽 메시지를 소비할 준비가 되어있다. Flux에 의해 들어오는 각 inbounded message는 ReceriverRecord로 나타난다. 각 ReceiverRecord은 KafkaConsumer가 committable ReceiverOffset 인스턴스와 함께 반환한 ConsumerRecord이다.

확인(Ack) 되지 않은 오프셋은 커밋되지 않기 때문에 메시지가 처리된 후에는 확인해야한다. 커밋 구간이나 커밋 배치 사이즈가 설정되어있다면, 확인된 오프셋은 주기적으로 커밋된다. 오프셋은 커밋 작업에 대한 세밀한 관리가 필요하다면 `ReceiverOffset#commit()` 메서드를 사용하여 명시적으로 커밋을 할 수도 있다. 

```kotlin
inboundedFlux.subscrbe { r ->
    println("Received message: $r")
    r.receiverOffset().acknowledge()
}
```





### 와일드카드 패턴으로 구독

이때까지 예시는 하나의 토픽만을 구독하는 것에 대한 거였고, `ReceiverOptions#subscription()` 메서드가 제공하는 컬렉션에 여러 토픽을 지정함으로써 하나 이상의 토픽을 구독할 때도 같은 API를 사용할 수 있다. subscription 메서드는 구독할 토픽에 패턴을 지정함으로써 와일드카드를 사용하여 토픽을 설정할 수도 있다. KafkaConsumer의 그룹 관리는 패턴과 일치하는 토픽이 생성되거나 삭제될 때 토픽 할당을 동적으로 업데이트하고 사용 가능한 컨슈머 인스턴스에 일치하는 토픽의 파티션을 할당한다.

```kotlin
receiverOptions = receiverOptions.subscription(Pattern.compile("demo.*"))
```

Receiver 인스턴스가 생성되기 전에 RecieverOptions를 변경해야한다. 토픽 구독을 변경하면 옵션 인스턴스의 기존 토픽 구독이 삭제된다.



### 토픽 파티션 수동 할당

파티션을 Kafka 컨슈머 그룹 관리를 사용하지 않고 receiver에 수동으로 할당할 수도 있다. 

```kotlin
receiverOptions = receiverOptions.assignment(Collections.singleton(TopicPartition(topic, 0)))
```

할당이 새로 정의되면 기존의 구독과 할당된 옵션들은 삭제된다. 옵션 인스턴스로부터 수동 할당으로 생성된 모든 receiver는 지정된 모든 파티션의 메시지를 소비한다.



### 커밋 주기 제어

`commit interval` 과 `commit batch size` 를 사용하여 커밋 주기를 제어할 수 있다. 각 구간 마다 또는 batch size만큼 쌓이면 커밋을 수행한다. 이 옵션들은 receiver가 만들어지기 전에 `ReceiverOptions` 에서 설정할 수 있다. 커밋 구간을 설정하면, 그 구간 내에 하나라도 레코드를 소비한다면 최소 한번의 커밋이 예약된다. 커밋 batch size가 설정된다면, 소비된 레코그닥 batch size 만큼 확인할 때 커밋이 예약된다. 설정된 커밋 주기에 기반한 자동 커밋과 레코드를 처리한 후 소비된 레코드를 수동으로 ack 하면 적어도 한 번 이상의 `delivery semantics` 이 가능하다.

컨슈머 어플리케이션이 메시지를 발견한 후 메시지가 처리되고 확인 되기 전에 크래시가나면 메시지를 다시 전달받는다. `ReceiverOffset#acknowledge()` 메서드를 사용하여 명시적으로 확인된 오프셋만이 커밋된다. Offset을 확인하면 같은 파티션의 모든 이전 Offset을 확인한다. 파티션이 재조정 중에 파티션이 취소되고 recieve Flux가 종료될 때 확인된 모든 오프셋들이 커밋된다.

커밋 작업 타이밍을 세부적으로 제어해야하는애플리케이션은 주기적인 커밋을 사용하지 않도록 하고 커밋을 트리거하는 데 필요한 경우 `ReceiverOffset#commit()` 을 명시적으로 호출할 수 있다. 이 커밋은 기본적으로 비동기 방식으로 돌아가지만, 애플리케이션은 대부분 동기 커밋을 구현하기 위해 반환된 Mono에서 `Mono#block()` 메서드를 호출한다. 애플리케이션은 메시지를 소비할 때 ack하고 ack된 오프셋을 커밋하기 위해 주기적으로 commit()을 호출하여 커밋을 일괄 처리할 수 있다.

```kotlin
receiver.receive()
    .doOnNext { r ->
    		process(r)
        r.receiveOffset().commit().block()
    }
```

오프셋을 커밋하면 해당 파티션의 모든 이전 오프셋을 확인하고 커밋한다. 확인된 모든 오프셋은 파티션이 재조정 중에 취소될 때와 receive Flux가 종료될 때 커밋된다.



### 레코드 배치 자동 확인(ack)

`KafkaReceiver#receiveAutoAck` 메서드는 각 `KafkaConsumer#poll()` 메서드에 의해 반환된 레코드 배치 Flux를 반환한다. 각 배치에서의 레코드들은 배치에 해당하는 Flux가 종료될 때 자동으로 확인된다.

```kotlin
KafkaReceiver.create(receiverOptions)
    .receiveAutoAck()
    .concatMap { r -> r }
    .subscribe { r -> println("Received: $r") }
```

각 배치에서의 최대 레코드 수는 `KafkaConsumer` 속성의 `MAX_POLL_RECORDS` 를 사용하여 제어할 수 있다. 이 값은 각 폴링에서 Kafka broker로부터 가져온 데이터의 양을 제어하기 위해 KafkaConsumer에 설정된 `fetch size` 와 `wait times` 와 함께 사용된다. 각 배치는 Flux가 종료된 후 확인된 Flux로 반환된다. 확인된 레코드들은 설정된 `commit interval` 과 `batch size` 에 기반하여 주기적으로 커밋된다. 이 모드는 애플리케이션이 어떠한 확인 및 커밋 작업을 신경쓰지 않아도 되기 때문에 사용하기 간단하다. 이는 효율적이면서 메시지를 최소 한 번 전달하는 데 사용할 수 있다.



### 자동 커밋 사용안함 설정

Kafka로 offset을 커밋할 필요 없는 애플리케이션은 `KafkaReceiver#receive()` 를 사용하여 소비된 어떤 레코드도 ack를 하지 않음으로써 auto commit을 사용안함으로 설정할 수 있다. 

```kotlin
receiverOptions = ReceiverOptions.<Int, String>create()
    .commitInterval(Duration.ZERO) // 커밋 주기 사용 안함
    .commitBatchSize(0) // 커밋 배치사이즈 사용 안함

KafkaReceiver.create(receiverOptions)
    .receive()
    .subscribe { process(it) } // process는 하지만 ack는 하지 않는다.
```



### 최대 한 번 delivery

레코드를 re-delivery하는 것을 방지하기 위해 auto commit을 사용하지 않는 애플리케이션도 있다. 새로운 레코드만을 소비하기 위해 `ConsumerConfig#AUTO_OFFSET_RESET_CONFIG` 를 `latest` 로 설정할 수 있다. 그러나 이는 애플리케이션이 실패하고 재시작할 때 예상치 못하게 일부 레코드들이 소비되지 않을 수 있다. 

`KafkaReceiver#receiveAtmostOnce` 는 애플리케이션이 실패하거나 충돌하는 경우 손실될 수 있는 설정 가능한 파티션 당 레코드 수로 최대 한 번의 의미를 가진 레코드를 소비하는 데 사용할 수 있다. 오프셋은 해당 레코드가 전송되기 전에 동기적으로 커밋된다. 레코드들은 컨슈머 애플리케이션이 실패해도 재전송되지 않도록 보장된다. 그러나 커밋 이후 레코드가 처리되기 전에 애플리케이션이 실패하면 몇몇 레코드들이 처리되지 않을 수 있다. 

각 레코드들이 개별적으로 커밋되고 커밋 연산이 성공되기 전 까지 전달되지 않기 때문에 이 모드는 비용이 든다. `ReceiverOptions#atmostOnceCommitCommitAheadSize` 는 커밋 비용을 줄이고 레코드의 오프셋이 이미 커밋된 경우 디스패치하기 전에 차단을 피하도록 설정할 수 있다. 기본적으로, commit-ahead는 사용할 수 없고, 애플리케이션이 크래쉬가 발생하는 경우, 각 파티션마다 적어도 하나의 레코드는 손실된다. 만약 commit-ahead가 설정되었다면, 파티션 마다 손실될 레코드의 최대 갯수는 `ReceiverOptions#atmostOnceCommitCommitAheadSize + 1` 이다.

```kotlin
KafkaReceiver.create(receiverOptions)
    .receiveAtmostOnce()
    .subscribe { println("Received: $it") } 
```





### 파티션 할당과 리스너 해지

애플리케이션은 파티션을 컨슈머에게 할당하거나 해지할 때 어떤 조치를 하기위해서 할당 및 해지 리스너가 모든 작업을 수행하도록 할 수 있다. 

그룹 관리를 사용할 때, rebalance 후에 컨슈머에게 파티션을 할당받을 때 마다 할당 리스너가 호출된다. 수동 할당을 사용하는 경우, 컨슈머 애플리케이션이 시작되면  할당 리스너가 호출된다. 할당 리스너는 할당된 파티션에서 특정 오프셋을 찾기위해 사용될 수 있다. 그래서 메시지는 특정한 오프셋 부터 소비된다.

그룹 관리를 사용할 때, rebalance 후에 컨슈머로부터 파티션이 해지될 때 마다 해지 리스너가 호출된다. 수동 할당을 사용하는 경우, 해지 리스너는 컨슈머가 종료되기 전에 호출된다. 수동 커밋을 사용하는 경우 처리된 오프셋을 커밋하기 위해 해지 리스너는 를 사용할 수 있다. 자동 커밋이 활성화된 경우 확인된 오프셋들은 자동으로 커밋된다.



### 레코드 소비를 위한 시작 오프셋 제어

기본적으로, receiver는 각 할당된 파티션의 마지막으로 커밋된 오프셋으로 부터 레코드를 소비하기 시작한다. 커밋된 오프셋을 이용할 수 없는 경우, 시작 오프셋을 파티션의 가장 최근의 오프셋이나 가장 이른 오프셋으로 설정하기 위해  `KafkaConsumer` 에 설정된 `ConsumerConfig#AUTO_OFFSET_RESET_CONFIG` 의 오프셋 리셋 전략을 사용한다. 애플리케이션은 할당 리스너에서 새 오프셋을 찾아 오프셋을 재정의 할 수 있다. `ReceiverPartition` 에는 파티션에서 가장 이르거나 최신이거나 아니면 특정 오프셋을 찾는 메서드가 제공된다. 

```java
void seekToBefinning();
void seekToEnd();
voud seek(long offset);
```

아래 코드는 가장 최신의 오프셋으로부터 메시지를 소비하는 옵션 예시이다.

```kotlin
receiverOptions = receiverOptions.addAssignListener { it.forEach { p -> p.seekToEnd() } }
    .subscription(Collections.singleton(topic))

KafkaReceiver.create(receiverOptions).receive().subscribe()
```



### 컨슈머 라이프사이클

각 `KafkaReceiver` 인스턴스는 KafkaReceiver의 수신 방법 중 하나에서 반환된 inboundFlux가 구독될 때 생성되는  `KafkaConsumer` 와 관련되어있다. 컨슈머는 Flux가 완료될 때 까지 유지한다. Flux가 완료되는 경우, 모든 확인된 오프셋이 커밋되고 컨슈머가 종료된다.

KafkaReceiver에서는 한 번에 하나의 수신 작업만 활성화될 수 있다. 모든 수신 메서드는 마지막 수신에 해당하는 Flux가 종료된 후에 호출될 수 있다.







## 이슈 모음집

https://github.com/reactor/reactor-kafka/issues/51

https://stackoverflow.com/questions/26678208/spring-boot-shutdown-hook/26678606



```
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" 
   "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="ko"  xml:lang="ko">
<meta http-equiv="Content-Type" Content="text/html; charset=utf-8" />
<meta name="viewport" content="user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, width=device-width, user-scalable=0">
    
<!--#메일 전체 div-->
<div style="max-width: 700px; font-family: '나눔고딕',NanumGothic,'맑은고딕',Malgun Gothic,dotum,'돋움',Dotum,Helvetica; width: 100%; background: #fff; letter-spacing: -1px; padding: 0px; margin: 0px auto;">
    <!--#메일헤더-->
    <div style="padding: 24px 0px 10px 0px; text-align: left">
        <!--#헤더타입 : 로고이미지-->
        <img style="height: 24px" alt="샵바이" src="http://image.toast.com/aaaaahb/readyshop/email/shop%20by_logo_email.png">
        <!--#헤더타입 : 텍스트-->
        <!--#메일제목-->
        <h2 style="font-weight: normal; font-size: 23px; color: #000; margin: 0 auto; padding: 10px 0px 10px 0px; text-align: left; line-height: 30px"><span style="font-weight: bold">OTP를 재설정해주세요</span></h2>
    </div>
    <!--#메일내용 시작-->
    <div style="width: 100%; padding: 0px; margin: 0px;" cellpadding="0" cellpadding="0" border="0">
        <!--#메일본문 :들어가는말-->
        <div style="font-size: 13px; padding: 20px 0px 10px 0px; border-top: #e5e5e5 2px solid">
            <p style="font-size: 13px; color: #555; text-align: left; padding: 0px; margin: 0px; line-height: 26px;">
                ${admin.adminId}님, OTP 재설정을 위한 OTP 키 입니다.
            </p>
        </div>

        <!--#메일본문 : 진짜 내용-->
        <div>
            <!--#테이블 : 내용이 아래로 내려가는 타입-->
                <table style="font-size: 13px; color: #555; width: 100%; text-align: left; border-top: #e5e5e5 2px solid; border-bottom: #e5e5e5 2px solid; margin: 5px 0px 10px 0px;" cellspacing="0" cellpadding="0">
                    <tr>
                        <th style="width: 150px; padding: 8px; background-color:#efefef;">OTP 키</th>
                        <td style="padding: 8px; letter-spacing: 0px">${otpSecret}</td>
                    </tr>
                </table>

        <!--#메일본문 : 첨언내용-->
        <div style="font-size: 13px; color: #555; text-align: left; padding: 20px 0px 20px 0px; line-height: 17px; border-top: #e5e5e5 0px solid;">
            <p style="padding: 0px 0px 0px 10px; margin: 0px; line-height: 26px;">
                <span style="font-weight: bold; margin: 0px 10px 0px -7px">①</span>설정 시작하기 > 직접 입력을 선택합니다.<br>
                <span style="font-weight: bold; margin: 0px 10px 0px -7px">②</span>이메일 주소와 발급된 OTP 키를 입력합니다.<br>
                <span style="font-weight: bold; margin: 0px 10px 0px -7px">③</span>시간 기준을 사용 설정했는지 확인한 후 설정을 완료해주세요.
            </p>
        </div>
    </div>
    <!--#메일푸터-->
        <div style="font-size: 11px; color: #666666; padding: 25px 20px 25px 20px; line-height: 16px; background-color: #ebebeb">
            본 메일은 발신전용으로 문의사항은 <a href="https://www.nhn-commerce.com/mygodo/helper_write.html?src=email&kw=000052&ACENO=20">1:1 문의하기</a> 또는 고객센터 1688-7662를 이용해 주시기 바랍니다.<br data-tomark-pass><br data-tomark-pass>
            <span style="display: block;font-size: 12px; color: #222; text-align: left">ⓒ NHN COMMERCE<strong style="color:#f91d11;">:</strong> Corp. All Rights Reserved.</span>
        </div>
    </div>
</div>
</html>
```

