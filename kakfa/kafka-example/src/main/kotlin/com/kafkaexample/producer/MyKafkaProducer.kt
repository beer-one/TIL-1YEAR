package com.kafkaexample.producer

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.slf4j.LoggerFactory
import java.util.concurrent.Future

object MyKafkaProducer {

    private val producer: KafkaProducer<String, String>
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        val properties = mapOf(
            "bootstrap.servers" to "192.168.0.4:9092",
            "key.serializer" to "org.apache.kafka.common.serialization.StringSerializer",
            "value.serializer" to "org.apache.kafka.common.serialization.StringSerializer"
        )

        producer = KafkaProducer(properties)
    }

    fun send(record: ProducerRecord<String, String>): Future<RecordMetadata> {
        return producer.send(record)
    }

    fun send(record: ProducerRecord<String, String>, callback: (RecordMetadata?, Exception?) -> Unit) {
        producer.send(record, callback)
    }
}