package com.kafkaexample.consumer

import org.apache.kafka.clients.consumer.CommitFailedException
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class MyKafkaConsumer(
    private val groupId: String,
    private val consumerNo: Int,
    topics: List<String>,
    private val autoCommitEnabled: Boolean = true
) {
    private val consumer: KafkaConsumer<String, String>
    private val customerCountryMap: MutableMap<String, Int> = mutableMapOf()

    companion object {
        private val TIMEOUT = Duration.ofMillis(100)
        private val logger = LoggerFactory.getLogger(javaClass)
        private var sequence = AtomicInteger(0)

        fun create(groupId: String, topics: List<String>, autoCommitEnabled: Boolean = true) = MyKafkaConsumer(
            groupId = groupId,
            consumerNo = sequence.getAndIncrement(),
            topics = topics,
            autoCommitEnabled = autoCommitEnabled
        )
    }

    init {
        val properties = mapOf(
            "bootstrap.servers" to "192.168.0.4:9092",
            "key.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
            "value.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
            "group.id" to groupId,
            "enable.auto.commit" to autoCommitEnabled.toString()
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

                if (!autoCommitEnabled) {
                   consumer.commitAsync(commitCallback)
                }
            }
        }
    }

    private fun process(record: ConsumerRecord<String, String>) {
        logger.info("$consumerName Topic: ${record.topic()}, partition = ${record.partition()}, offset = ${record.offset()}, key = ${record.key()}, value = ${record.value()}")

        val updateCount = (customerCountryMap[record.value()]?: 0) + 1
        customerCountryMap[record.value()] = updateCount

        logger.info("$consumerName Current: $customerCountryMap")
    }

    private val consumerName = "[Consumer $consumerNo]"

    private val commitCallback = { offset: Map<TopicPartition, OffsetAndMetadata>, e: Exception ->
        logger.error("$consumerName Commit failed for offset $offset", e)
    }
}