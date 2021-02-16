package com.kafkaexample.producer

import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger("logger")

fun main() {
    asynchronousSend()
}

fun fireAndForget() {
    val record = ProducerRecord("beer", "Heineken", "Nederland")

    try {
        MyKafkaProducer.send(record)
    } catch (e: Exception) {
        // BufferExhaustedException or TimeoutException - 버퍼 꽉 참
        // SerializationException - 직렬화 실패
        // InterruptException - 스레드 중단
        e.printStackTrace()
    }


}

fun synchronousSend() {
    val record = ProducerRecord("beer", "Hoegaarden", "Belgium")

    try {
        val metadata = MyKafkaProducer.send(record)
            .get()
        logger.info("offset: ${metadata.offset()}, partition: ${metadata.partition()}")

    } catch (e: Exception) {
        e.printStackTrace()
    }
}

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