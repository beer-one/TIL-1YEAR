package com.floidea.example.subscriber

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.reactivestreams.Subscription
import reactor.core.publisher.BaseSubscriber

class TestSubscriber<T>: BaseSubscriber<T>() {
    var time = System.currentTimeMillis()

    override fun hookOnSubscribe(subscription: Subscription) {
        println("Subscribed")
        time = System.currentTimeMillis()
        request(1)
    }

    override fun hookOnNext(value: T) {
        println("onNext")
        val fetchTime = System.currentTimeMillis() - time
        println("value: $value, fetchTime: $fetchTime")
        time = System.currentTimeMillis()

        runBlocking { delay(1000L) }
        request(1)
    }
}