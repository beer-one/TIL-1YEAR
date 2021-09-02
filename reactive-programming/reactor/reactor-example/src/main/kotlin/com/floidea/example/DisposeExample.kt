package com.floidea.example

import kotlinx.coroutines.*
import org.reactivestreams.Subscription
import reactor.core.Disposable
import reactor.core.publisher.BaseSubscriber
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SignalType
import reactor.core.scheduler.Schedulers
import reactor.util.context.Context
import reactor.util.function.Tuples
import java.lang.RuntimeException
import java.util.function.LongConsumer
import kotlin.coroutines.CoroutineContext

fun main() {
    val sampleSubscriber = SampleSubscriber<Int>(4)
    val intFlux = Flux.range(1, 10)

    intFlux.subscribe(sampleSubscriber)
}

class SampleSubscriber<T>(
    private val exitValue: T
): BaseSubscriber<T>() {

    override fun hookOnSubscribe(subscription: Subscription) {
        println("Subscribed")
        request(1)
    }

    override fun hookOnNext(value: T) {
        println(value)

        if (value != exitValue)
            request(1)
    }

    override fun hookOnError(throwable: Throwable) { }

    override fun hookOnCancel() { }

    override fun hookOnComplete() { }

    override fun hookFinally(type: SignalType) { }
}
