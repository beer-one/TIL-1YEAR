package com.kafkaexample.consumer

import java.util.concurrent.Executors

fun main() {
    val executor = Executors.newFixedThreadPool(ConsumerConfig.CONSUMER_NUM)

    val consumers = (1..ConsumerConfig.CONSUMER_NUM).map {
        MyKafkaConsumer.create(ConsumerConfig.GROUP_ID, ConsumerConfig.TOPICS, autoCommitEnabled = false)
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